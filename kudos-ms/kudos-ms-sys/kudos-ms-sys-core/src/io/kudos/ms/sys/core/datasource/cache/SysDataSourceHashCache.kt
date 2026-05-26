package io.kudos.ms.sys.core.datasource.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.cache.SysDataSourceHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.datasource.dao.SysDataSourceDao
import io.kudos.ms.sys.core.datasource.event.SysDataSourceBatchDeleted
import io.kudos.ms.sys.core.datasource.event.SysDataSourceDeleted
import io.kudos.ms.sys.core.datasource.event.SysDataSourceInserted
import io.kudos.ms.sys.core.datasource.event.SysDataSourceUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Unified Hash cache handler for data sources, supporting lookup by id and by tenant id + sub-system code + micro-service code.
 *
 * 1. Source table: sys_data_source
 * 2. Lookup by primary id: caches all data sources (including active=false)
 * 3. Lookup by tenant id + sub-system code + micro-service code: only records with non-null tenantId
 * 4. Hash structure keyed by id; secondary indexes on tenantId, subSystemCode, microServiceCode
 *
 * Before use, add an entry named [CACHE_NAME] with hash=true in the cache config table sys_cache.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysDataSourceHashCache : AbstractHashCacheHandler<SysDataSourceCacheEntry>() {

    @Resource
    private lateinit var sysDataSourceDao: SysDataSourceDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_DATA_SOURCE__HASH"

        /** Filterable secondary properties used to build indexes by tenantId/subSystemCode/microServiceCode */
        val FILTERABLE_PROPERTIES = setOf(
            SysDataSourceCacheEntry::tenantId.name,
            SysDataSourceCacheEntry::subSystemCode.name,
            SysDataSourceCacheEntry::microServiceCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysDataSourceCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysDataSourceCacheEntry? =
        sysDataSourceDao.get(id.toString(), SysDataSourceCacheEntry::class)

    // ---------- Lookup by primary id (equivalent to DataSourceByIdCache) ----------

    /**
     * Gets a data source by id from cache; on miss, queries the database and writes back.
     *
     * @param id data source primary key, non-blank
     * @return cached data source entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysDataSourceCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "subSystemCode", "microServiceCode"]
    )
    open fun getDataSourceById(id: String): SysDataSourceCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching a data source" }
        return sysDataSourceDao.get(id, SysDataSourceCacheEntry::class)
    }

    /**
     * Batch gets data sources by ids from cache; misses are loaded from the database and written back.
     *
     * @param ids data source id collection, may be empty
     * @return id -> cached entry map; only contains ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysDataSourceCacheEntry::class,
        filterableProperties = ["tenantId", "subSystemCode", "microServiceCode"]
    )
    open fun getDataSourcesByIds(ids: List<String>): Map<String, SysDataSourceCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysDataSourceDao.fetchDataSourcesByIdsForCache(ids)
        return list.filter { it.id.isNotBlank() && it.id in ids }.associateBy { it.id }
    }

    // ---------- Lookup by tenant id + sub-system code + micro-service code (equivalent to DataSourceByTenantIdAnd3CodesCache) ----------

    /**
     * Equality query by tenant id, sub-system code and micro-service code, returning the matching data sources.
     * Checks the secondary index in cache first; on miss, queries the database and writes back.
     *
     * @param tenantId tenant id, non-blank
     * @param subSystemCode sub-system code, may be null
     * @param microServiceCode micro-service code, may be null
     * @return matching cache entries
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#subSystemCode", "#microServiceCode"],
        entityClass = SysDataSourceCacheEntry::class,
        filterableProperties = ["tenantId", "subSystemCode", "microServiceCode"]
    )
    open fun getDataSources(
        tenantId: String,
        subSystemCode: String?,
        microServiceCode: String?
    ): List<SysDataSourceCacheEntry> {
        require(tenantId.isNotBlank()) { "tenant id must be provided when fetching data sources" }
        return sysDataSourceDao.fetchDataSourcesForCache(tenantId, subSystemCode, microServiceCode)
    }


    // ---------- Full refresh and synchronization ----------

    /**
     * Loads all data sources from the database and refreshes the Hash cache (contains all records, same as the by-id cache).
     *
     * @param clear when true, clears before writing; when false, overwrites in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skip loading data source Hash cache.")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysDataSourceDao.searchAs<SysDataSourceCacheEntry>()
        log.debug("Loaded ${list.size} data sources from database; refreshing Hash cache.")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Syncs after data source insert: loads the entity by id from the database and writes it to the cache.
     *
     * @param any object carrying needed attributes (used to decide whether to write the 3-codes index)
     * @param id data source id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysDataSourceDao.get(id, SysDataSourceCacheEntry::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Syncs after data source update: removes the old index entries, then writes back with the new data.
     *
     * @param any object carrying needed attributes
     * @param id data source id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysDataSourceDao.get(id, SysDataSourceCacheEntry::class) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Syncs after updating active status: reloads the entity by id and writes it back (the index will be updated accordingly).
     *
     * @param id data source id
     * @param active enabled flag
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysDataSourceDao.getAs<SysDataSourceCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Syncs after data source delete: removes the id and its secondary index entries from the cache.
     *
     * @param id data source id
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysDataSourceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Syncs cache after batch database delete.
     *
     * @param ids primary key collection
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        log.debug("After batch deleting sys_data_source with ids $ids, evicting from ${cacheName()} cache...")
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysDataSourceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
        log.debug("${cacheName()} cache sync completed.")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDataSourceInserted): Unit = syncOnInsert(event, event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDataSourceUpdated): Unit = syncOnUpdate(event, event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDataSourceDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDataSourceBatchDeleted): Unit = syncOnBatchDelete(event.ids)

}
