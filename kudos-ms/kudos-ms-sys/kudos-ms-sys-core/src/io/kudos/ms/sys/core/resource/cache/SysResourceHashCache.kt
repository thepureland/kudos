package io.kudos.ms.sys.core.resource.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.event.SysResourceBatchDeleted
import io.kudos.ms.sys.core.resource.event.SysResourceDeleted
import io.kudos.ms.sys.core.resource.event.SysResourceInserted
import io.kudos.ms.sys.core.resource.event.SysResourceUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Unified system resource cache handler backed by a Hash structure storing [SysResourceCacheEntry].
 *
 * Provides three query and write-back patterns:
 * - **By primary key**: fetch single or batch entities by id.
 * - **By subsystem + URL**: fetch a single resource id or entity list by subsystem code, URL and active state.
 * - **By subsystem + resource type**: fetch resource id list or entity list by subsystem code, resource type and active state.
 *
 * Secondary properties listed in [FILTERABLE_PROPERTIES] are used to build Set indexes for multi-condition equality queries;
 * writes, deletes and full reloads must all use the same set of secondary properties to keep the indexes consistent.
 *
 * Before use, add a config item named [CACHE_NAME] to the cache configuration table sys_cache.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysResourceHashCache : AbstractHashCacheHandler<SysResourceCacheEntry>() {

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_RESOURCE__HASH"

        /** Secondary property names used for equality filtering and Set indexes; writes/deletes/full reloads must stay consistent (active not included, no secondary index on active) */
        val FILTERABLE_PROPERTIES = setOf(
            SysResourceCacheEntry::subSystemCode.name,
            SysResourceCacheEntry::url.name,
            SysResourceCacheEntry::resourceTypeDictCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysResourceCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysResourceCacheEntry? = sysResourceDao.getAs(id.toString())

    // ---------- 1. By primary key id ----------

    /**
     * Fetch a single resource entity by primary key id.
     * Reads from cache first; on miss, loads from database and writes back, building secondary property indexes per [FILTERABLE_PROPERTIES].
     *
     * @param id resource primary key, non-blank
     * @return resource cache entry, or null when not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysResourceCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourceById(id: String): SysResourceCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching a resource" }
        return sysResourceDao.getAs<SysResourceCacheEntry>(id)
    }

    /**
     * Batch fetch resource entities by primary key id list.
     * Reads from cache first; ids that miss are loaded from database and written back, building secondary property indexes per [FILTERABLE_PROPERTIES].
     *
     * @param ids resource primary key list, may be empty
     * @return id -> entity mapping, containing only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysResourceCacheEntry::class,
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourcesByIds(ids: Set<String>): Map<String, SysResourceCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysResourceDao.getByIdsAs<SysResourceCacheEntry>(ids)
        val byId = list.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.let { id to it } }.toMap()
    }

    // ---------- 2. By subsystem + URL ----------

    /**
     * Multi-condition equality query by subsystem code, URL and active state; returns the matching resource entity list (0 or 1 row).
     * Queries by secondary property index first; on miss, loads from database and writes back.
     *
     * @param subSystemCode subsystem code, non-blank
     * @param url resource URL, non-blank
     * @return matching entity list
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#subSystemCode", "#url"],
        entityClass = SysResourceCacheEntry::class,
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourceBySubSystemCodeAndUrl(subSystemCode: String, url: String): SysResourceCacheEntry? {
        return sysResourceDao.fetchResourceBySubSysAndUrl(subSystemCode, url)
    }

    // ---------- 3. By subsystem + resource type ----------

    /**
     * Multi-condition equality query by subsystem code, resource type and active state; returns the matching resource entity list.
     * Queries by secondary property index first; on miss, loads from database and writes back.
     *
     * @param subSystemCode subsystem code, non-blank
     * @param resourceTypeDictCode resource type dictionary code, non-blank
     * @return matching entity list
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#subSystemCode", "#resourceTypeDictCode"],
        entityClass = SysResourceCacheEntry::class,
        filterableProperties = ["subSystemCode", "url", "resourceTypeDictCode"]
    )
    open fun getResourcesBySubSystemCodeAndType(subSystemCode: String, resourceTypeDictCode: String): List<SysResourceCacheEntry> {
        return sysResourceDao.searchBySubSysAndType(subSystemCode, resourceTypeDictCode)
    }

    // ---------- Full reload ----------

    /**
     * Load all resources from database and refresh the Hash cache.
     *
     * @param clear when true, clears the current cache before writing; when false, overwrites in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skip loading resource Hash cache")
            return
        }
        val cache = hashCache()
        val list = sysResourceDao.searchAs<SysResourceCacheEntry>()
        log.debug("Loaded ${list.size} resources from database, refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("Resource Hash cache refresh completed")
    }

    // ---------- Post-write sync (called by business code after insert/update/delete) ----------

    /**
     * Sync after insert: load the entity for the specified id from database, write to cache and build secondary property indexes.
     *
     * @param id primary key of the newly inserted resource
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysResourceDao.getAs<SysResourceCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Sync after insert (overload, accepts business object and id). Same behavior as [syncOnInsert(id)].
     *
     * @param any business object, used only for overload disambiguation
     * @param id primary key of the newly inserted resource
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Sync after update: reload the entity for the specified id from database, write to cache and update secondary property indexes.
     *
     * @param id primary key of the updated resource
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysResourceDao.getAs<SysResourceCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Sync after update (overload, includes old URL parameter). Same behavior as [syncOnUpdate(id)]; overwriting suffices under the Hash structure.
     */
    open fun syncOnUpdate(any: Any, id: String, oldUrl: String?) {
        syncOnUpdate(id)
    }

    /**
     * Sync after update (overload, includes old subsystem and resource type parameters). Same behavior as [syncOnUpdate(id)].
     */
    open fun syncOnUpdate(any: Any, id: String, oldSubSystemCode: String, oldResourceTypeDictCode: String) {
        syncOnUpdate(id)
    }

    /**
     * Sync after updating a resource's active state. Same behavior as [syncOnUpdate(id)].
     *
     * @param id resource primary key
     * @param active new active state
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        syncOnUpdate(id)
    }

    /**
     * Sync after updating active state (overload without the active parameter). Same behavior as [syncOnUpdate(id)].
     */
    open fun syncOnUpdateActive(id: String) {
        syncOnUpdate(id)
    }

    /**
     * Sync after delete: remove the id from cache and from secondary property Set indexes.
     *
     * @param id primary key of the deleted resource
     * @param subSystemCode subsystem code the resource belongs to (used for index removal)
     * @param urlOrResourceType URL or resource type, used only for index removal, may be null
     */
    open fun syncOnDelete(id: String, subSystemCode: String, urlOrResourceType: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysResourceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Sync after batch delete: remove the ids from cache and from secondary property Set indexes.
     *
     * @param ids primary keys of deleted resources
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysResourceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    // region Event subscriptions (dispatched by SysResourceService after transaction commit) ---------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysResourceInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysResourceUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysResourceDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, event.id, SysResourceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysResourceBatchDeleted): Unit = syncOnBatchDelete(event.ids)

    // endregion

    /**
     * Build a composite key on the "subsystem + URL" dimension; format: subsystem code + delimiter + URL.
     * Used by external callers that need to align with the cache key convention.
     */
    fun getKeySubSysAndUrl(subSystemCode: String, url: String?): String {
        return "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${url}"
    }

    /**
     * Build a composite key on the "subsystem + resource type" dimension; format: subsystem code + delimiter + resource type dictionary code.
     * Used by external callers that need to align with the cache key convention.
     */
    fun getKeySubSysAndType(subSystemCode: String, resourceTypeDictCode: String): String {
        return "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${resourceTypeDictCode}"
    }
}
