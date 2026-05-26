package io.kudos.ms.sys.core.tenant.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.tenant.vo.SysTenantSystemCacheEntry
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.tenant.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemBound
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemSystemsChanged
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemTenantsChanged
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Unified cache handler for tenant-system relationships, stored using a Hash structure.
 *
 * Source table: sys_tenant_system
 *
 * Provides query and write-back by secondary properties:
 *  1. system code -> Set of tenant ids
 *  2. tenant id -> Set of system codes
 *
 * Uses the secondary properties in [FILTERABLE_PROPERTIES] to build Set indexes, supporting multi-condition equality queries;
 * all writes, deletes, and full refreshes must use the same secondary property set to keep the index consistent.
 *
 * Before use, add a configuration named [CACHE_NAME] with hash=true in the sys_cache configuration table.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysTenantSystemHashCache : AbstractHashCacheHandler<SysTenantSystemCacheEntry>() {

    @Resource
    private lateinit var sysTenantSystemDao: SysTenantSystemDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_TENANT_SYSTEM__HASH"

        /** Filterable secondary properties, used to build secondary indexes by tenantId / systemCode */
        val FILTERABLE_PROPERTIES = setOf(
            SysTenantSystemCacheEntry::tenantId.name,
            SysTenantSystemCacheEntry::systemCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysTenantSystemCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysTenantSystemCacheEntry? =
        sysTenantSystemDao.get(id.toString(), SysTenantSystemCacheEntry::class)

    // ---------- By subsystem code / by tenant id ----------

    /**
     * Get the set of tenant ids by subsystem code.
     * Looks up the cache by secondary-property index first; on miss, queries the DB and writes back.
     *
     * @param systemCode system code, non-blank
     * @return set of tenant ids under this system
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#systemCode"],
        entityClass = SysTenantSystemCacheEntry::class,
        filterableProperties = ["tenantId", "systemCode"],
        returnProperty = "tenantId"
    )
    open fun getTenantIdsBySubSystemCode(systemCode: String): Set<String> {
        require(systemCode.isNotBlank()) { "systemCode must not be blank when querying by subsystem code" }
        val list = sysTenantSystemDao.fetchCacheItemsBySystemCode(systemCode)
        if (list.isNotEmpty() && KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().saveBatch(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        }
        return list.map { it.tenantId }.toSet()
    }

    /**
     * Get the set of system codes by tenant id.
     * Looks up the cache by secondary-property index first; on miss, queries the DB and writes back.
     *
     * @param tenantId tenant id, non-blank
     * @return set of system codes under this tenant
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId"],
        entityClass = SysTenantSystemCacheEntry::class,
        filterableProperties = ["tenantId", "systemCode"],
        returnProperty = "systemCode"
    )
    open fun getSubSystemCodesByTenantId(tenantId: String): Set<String> {
        require(tenantId.isNotBlank()) { "tenantId must not be blank when querying by tenant id" }
        val list = sysTenantSystemDao.fetchCacheItemsByTenantId(tenantId)
        if (list.isNotEmpty() && KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().saveBatch(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        }
        return list.map { it.systemCode }.toSet()
    }

    // ---------- Full refresh and sync ----------

    /**
     * Load all tenant-system relationships from the DB and refresh the Hash cache.
     *
     * @param clear when true, clear before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not active; skipping tenant-system relation Hash cache load")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysTenantSystemDao.fetchAllForCache()
        log.debug("Loaded ${list.size} tenant-system relations from DB; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-insert sync for a tenant-system relation: load the entity by id from the DB and write into the cache.
     *
     * @param id primary key
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysTenantSystemDao.get(id, SysTenantSystemCacheEntry::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-insert sync (overload taking a business object and id).
     *
     * @param any business object, used only to distinguish overloads
     * @param id primary key
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Post-update sync: reload the entity by id from the DB and write back to the cache.
     *
     * @param id primary key
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysTenantSystemDao.get(id, SysTenantSystemCacheEntry::class) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Post-update sync (overload).
     *
     * @param any business object, used only to distinguish overloads
     * @param id primary key
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * Post-delete sync: remove the id from the cache along with its secondary indexes.
     *
     * @param id primary key
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysTenantSystemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-batch-delete sync: remove these ids from the cache along with their secondary indexes.
     *
     * @param ids primary key collection
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        log.debug("After batch-deleting sys_tenant_system ids=$ids, evicting from ${cacheName()} cache...")
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysTenantSystemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
        log.debug("${cacheName()} cache sync complete.")
    }

    // region Event listeners ------------------------------------------------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantSystemBound): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantSystemTenantsChanged): Unit = syncOnBatchDelete(event.tenantIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantSystemSystemsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        event.systemCodes.forEach { systemCode ->
            evict(systemCode)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getTenantIdsBySubSystemCode(systemCode)
            }
        }
    }

    // endregion
}
