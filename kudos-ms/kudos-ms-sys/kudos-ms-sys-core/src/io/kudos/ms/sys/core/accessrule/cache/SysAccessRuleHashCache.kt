package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleCacheEntry
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Hash cache for `sys_access_rule`: full record stored under primary key id; secondary indexes built on
 * `systemCode` and `tenantId`. Supports equality lookups by id and by (systemCode + tenantId),
 * matching the table's unique constraint `(system_code, tenant_id)`.
 *
 * The tenant dimension is normalized uniformly via [AccessRuleTenantKey.normalize]: `null` / blank values map to
 * empty string (platform-level), consistent with the KV key in [AccessRuleIpsBySubSysAndTenantIdCache].
 *
 * Before use, [CACHE_NAME] must be configured in `sys_cache` with `hash = true`.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysAccessRuleHashCache : AbstractHashCacheHandler<SysAccessRuleCacheEntry>() {

    @Resource
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_ACCESS_RULE__HASH"

        /** Secondary attributes: Set indexes built on systemCode and tenantId (empty string = platform-level) */
        val FILTERABLE_PROPERTIES = setOf(
            SysAccessRuleCacheEntry::systemCode.name,
            SysAccessRuleCacheEntry::tenantId.name,
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysAccessRuleCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysAccessRuleCacheEntry? =
        sysAccessRuleDao.fetchCacheEntryById(id.toString())

    /**
     * Get access rule from cache by primary key; on miss, load from DB and write back.
     *
     * @param id Primary key, non-blank
     * @return Cache entry; `null` if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysAccessRuleCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["systemCode", "tenantId"],
    )
    open fun getAccessRuleById(id: String): SysAccessRuleCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching access rule by id" }
        return sysAccessRuleDao.fetchCacheEntryById(id)
    }

    /**
     * Get access rule from cache by system code and tenant id; on miss, load from DB and write back.
     *
     * @param systemCode System (sub-system) code, non-blank
     * @param tenantId Tenant id; `null` / blank is treated as platform-level (corresponds to `tenant_id IS NULL` in DB).
     *                 Internally normalized to empty string by [AccessRuleTenantKey.normalize] before secondary index matching.
     * @return The single row for this dimension on hit; `null` if not found
     */
    open fun getAccessRuleBySystemCodeAndTenantId(systemCode: String, tenantId: String?): SysAccessRuleCacheEntry? {
        require(systemCode.isNotBlank()) { "systemCode must not be blank when fetching access rule by system code" }
        // Call via self-proxy to ensure @HashCacheableBySecondary AOP takes effect;
        // tenantKey is normalized at the entry point so filterExpressions stays free of expressions (the framework requires single-arg SpEL).
        return getSelf<SysAccessRuleHashCache>()
            .findBySystemCodeAndTenantKey(systemCode, AccessRuleTenantKey.normalize(tenantId))
    }

    /**
     * Internal query entry point, only called by [getAccessRuleBySystemCodeAndTenantId]:
     * input [tenantKey] **must be normalized already** (`null` / blank converted to empty string) for `@HashCacheableBySecondary` secondary index matching.
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#systemCode", "#tenantKey"],
        entityClass = SysAccessRuleCacheEntry::class,
        filterableProperties = ["systemCode", "tenantId"],
    )
    open fun findBySystemCodeAndTenantKey(systemCode: String, tenantKey: String): SysAccessRuleCacheEntry? =
        sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId(systemCode, tenantKey)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skipping load of access rule Hash cache")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysAccessRuleDao.fetchAllCacheEntries()
        log.debug("Loaded ${list.size} access rules from database; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    // region Event subscription (dispatched by SysAccessRuleService after transaction commit) -------------------------------------

    /** Write to Hash cache after insert. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleInserted): Unit = syncOnInsert(event.id)

    /** After update, write back the latest row from DB. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleUpdated): Unit = syncOnUpdate(event.id)

    /** Evict cache after delete. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleDeleted): Unit = syncOnDelete(event.id)

    /** Evict cache one by one after batch delete. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleBatchDeleted): Unit = syncOnBatchDelete(event.ids)

    // endregion

    // region Sync primitives (kept as internal helpers for event listeners and existing tests) ----------------

    /** Write back to Hash cache by primary key. */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysAccessRuleDao.fetchCacheEntryById(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Reload by primary key and write back. */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysAccessRuleDao.fetchCacheEntryById(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** Remove entry and secondary indexes by primary key. */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysAccessRuleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Batch remove. */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysAccessRuleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    // endregion
}
