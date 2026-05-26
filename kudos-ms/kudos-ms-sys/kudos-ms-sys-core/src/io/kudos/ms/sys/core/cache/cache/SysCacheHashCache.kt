package io.kudos.ms.sys.core.cache.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.dao.SysCacheDao
import io.kudos.ms.sys.core.cache.event.SysCacheBatchDeleted
import io.kudos.ms.sys.core.cache.event.SysCacheDeleted
import io.kudos.ms.sys.core.cache.event.SysCacheInserted
import io.kudos.ms.sys.core.cache.event.SysCacheUpdated
import jakarta.annotation.Resource
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Hash cache handler for cache configurations, storing [io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry] via the Hash structure.
 *
 * Source table: sys_cache.
 *
 * Provides three query/write paths:
 * - **By primary key**: fetch single or batch entities by id.
 * - **By atomic service code + name**: fetch a single entry by atomicServiceCode and name.
 * - **By atomic service code**: fetch list by atomicServiceCode.
 *
 * Secondary indexes are built on the properties listed in [FILTERABLE_PROPERTIES] as Sets; every write, delete and full reload
 * must use the same secondary property set to keep indexes consistent.
 *
 * Before use, add a configuration row named [CACHE_NAME] (hash=true) in the sys_cache table.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysCacheHashCache : AbstractHashCacheHandler<SysCacheCacheEntry>() {

    @Resource
    private lateinit var sysCacheDao: SysCacheDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_CACHE__HASH"

        /** Set of secondary property names used for equality filtering and Set indexes; writes/deletes/full reloads must match this set. */
        val FILTERABLE_PROPERTIES = setOf(
            SysCacheCacheEntry::name.name,
            SysCacheCacheEntry::atomicServiceCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysCacheCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysCacheCacheEntry? = sysCacheDao.getAs(id.toString())

    // ---------- 1. By primary key id ----------

    /**
     * Fetch a single cache configuration by primary key id.
     * Looks up the cache first; on miss, queries the DB and writes back, building secondary indexes per [FILTERABLE_PROPERTIES].
     *
     * @param id cache configuration primary key, non-blank
     * @return cache entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysCacheCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCacheById(id: String): SysCacheCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching cache configuration" }
        return sysCacheDao.getAs<SysCacheCacheEntry>(id)
    }

    /**
     * Batch fetch cache configurations by primary key id list.
     * Looks up the cache first; for missed ids, queries the DB and writes back, building secondary indexes per [FILTERABLE_PROPERTIES].
     *
     * @param ids list of primary keys, may be empty
     * @return id -> entity map; contains only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysCacheCacheEntry::class,
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCachesByIds(ids: Set<String>): Map<String, SysCacheCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysCacheDao.getByIdsAs<SysCacheCacheEntry>(ids)
        val byId = list.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.let { id to it } }.toMap()
    }

    // ---------- 2. By atomic service code + name ----------

    /**
     * Fetch a single cache configuration by atomic service code and name.
     * Hits the secondary-index cache first; on miss, queries the DB and writes back.
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @param name cache name, non-blank
     * @return matching cache entry, or null if not found
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#name"],
        entityClass = SysCacheCacheEntry::class,
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCache(atomicServiceCode: String, name: String): SysCacheCacheEntry? {
        require(atomicServiceCode.isNotBlank()) { "atomicServiceCode must not be blank when fetching cache configuration" }
        require(name.isNotBlank()) { "name must not be blank when fetching cache configuration" }
        return sysCacheDao.fetchCacheEntryByNameAndAtomicServiceCode(atomicServiceCode, name)
    }

    // ---------- 3. By atomic service code ----------

    /**
     * Fetch the list of cache configurations matching an atomic service code.
     * Hits the secondary-index cache first; on miss, queries the DB and writes back.
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @return list of matching cache entries
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode"],
        entityClass = SysCacheCacheEntry::class,
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCaches(atomicServiceCode: String): List<SysCacheCacheEntry> {
        require(atomicServiceCode.isNotBlank()) { "atomicServiceCode must not be blank when fetching cache configuration" }
        return sysCacheDao.fetchCachesByAtomicServiceCode(atomicServiceCode)
    }

    // ---------- Full reload ----------

    /**
     * Load all cache configurations from the DB and refresh the Hash cache.
     *
     * @param clear when true, clear the current cache before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not active; skipping cache-configuration Hash cache load")
            return
        }
        val cache = hashCache()
        val list = sysCacheDao.searchAs<SysCacheCacheEntry>()
        log.debug("Loaded ${list.size} cache configurations from DB; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("Cache-configuration Hash cache refresh complete")
    }

    // ---------- Post-write sync (invoked by services after insert/update/delete) ----------

    /**
     * Post-insert sync: load the entity by id from the DB, write into the cache, and build secondary indexes.
     *
     * @param id primary key of the newly inserted cache configuration
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysCacheDao.getAs<SysCacheCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-insert sync (overload taking a business object and id). Behaves like [syncOnInsert(id)].
     *
     * @param any business object, used only to distinguish overloads
     * @param id primary key of the newly inserted cache configuration
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Post-update sync: reload the entity by id from the DB, write into the cache, and refresh secondary indexes.
     *
     * @param id primary key of the updated cache configuration
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysCacheDao.getAs<SysCacheCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Post-update sync (overload with a business object). Behaves like [syncOnUpdate(id)].
     *
     * @param any business object
     * @param id primary key of the updated cache configuration
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * Post-delete sync: remove the id from the cache and from the secondary Set indexes.
     *
     * @param id primary key of the deleted cache configuration
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysCacheCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-batch-delete sync: remove these ids from the cache and from the secondary Set indexes.
     *
     * @param ids primary keys of the deleted cache configurations
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysCacheCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    // ---------- Event listeners ----------
    // Uses plain @EventListener (not @TransactionalEventListener): sys_cache service tests assert the new cache state immediately
    // after a mutation, and AFTER_COMMIT does not fire in @Transactional rollback tests; therefore this domain keeps synchronous
    // event semantics, behaviorally equivalent to the old direct sync.

    @EventListener
    open fun on(event: SysCacheInserted): Unit = syncOnInsert(event.id)

    @EventListener
    open fun on(event: SysCacheUpdated): Unit = syncOnUpdate(event.id)

    @EventListener
    open fun on(event: SysCacheDeleted): Unit = syncOnDelete(event.id)

    @EventListener
    open fun on(event: SysCacheBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}