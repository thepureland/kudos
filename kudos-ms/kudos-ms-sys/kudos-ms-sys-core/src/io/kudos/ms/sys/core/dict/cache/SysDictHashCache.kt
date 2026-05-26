package io.kudos.ms.sys.core.dict.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.dict.cache.SysDictHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dict.dao.SysDictDao
import io.kudos.ms.sys.core.dict.event.SysDictBatchDeleted
import io.kudos.ms.sys.core.dict.event.SysDictDeleted
import io.kudos.ms.sys.core.dict.event.SysDictInserted
import io.kudos.ms.sys.core.dict.event.SysDictUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Dictionary type Hash cache handler, storing [SysDictCacheEntry] in a Hash structure.
 *
 * Data source table: sys_dict.
 *
 * Provides three categories of read and write-back capabilities:
 * - **By primary key**: get a single entity or batch by id.
 * - **By atomic service code + dictionary type**: fetch a single dictionary entity by atomicServiceCode and dictType.
 *
 * Uses the secondary properties in [FILTERABLE_PROPERTIES] to build Set indexes, supporting multi-condition equality queries.
 * All writes, deletes and full refreshes must use the same secondary property set to keep indexes consistent.
 *
 * Before use, an entry named [CACHE_NAME] with hash=true must be added to the sys_cache configuration table.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictHashCache : AbstractHashCacheHandler<SysDictCacheEntry>() {

    @Resource
    private lateinit var sysDictDao: SysDictDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_DICT__HASH"

        /** Secondary property names used for equality filtering and Set indexes; writes/deletes/full refreshes must match this set */
        val FILTERABLE_PROPERTIES = setOf(
            SysDictCacheEntry::atomicServiceCode.name,
            SysDictCacheEntry::dictType.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysDictCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysDictCacheEntry? = sysDictDao.getAs(id.toString())

    // ---------- 1. By primary key id ----------

    /**
     * Get a single dictionary entity by primary key id.
     * Queries the cache first; on miss, queries the database and writes back, building secondary indexes per [FILTERABLE_PROPERTIES].
     *
     * @param id dictionary primary key, non-blank
     * @return the cache entry, or null when not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysDictCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictById(id: String): SysDictCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching dictionary" }
        return sysDictDao.getAs<SysDictCacheEntry>(id)
    }

    /**
     * Batch get dictionary entities by primary key id list.
     * Queries the cache first; for any missing ids, queries the database and writes back, building secondary indexes per [FILTERABLE_PROPERTIES].
     *
     * @param ids list of dictionary primary keys, may be empty
     * @return id -> entity map, only including found ids
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysDictCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictsByIds(ids: Set<String>): Map<String, SysDictCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysDictDao.getByIdsAs<SysDictCacheEntry>(ids)
        val byId = list.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.let { id to it } }.toMap()
    }

    // ---------- 2. By atomic service code ----------

    /**
     * Query the dictionary entity list matching the given atomic service code.
     * Queries the cache via secondary indexes first; on miss, queries the database and writes back.
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @return list of matching dictionary entities
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode"],
        entityClass = SysDictCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictsByAtomicServiceCode(atomicServiceCode: String): List<SysDictCacheEntry> {
        return sysDictDao.searchDictsByAtomicServiceCode(atomicServiceCode)
    }

    // ---------- 3. By atomic service code + dictionary type ----------

    /**
     * Multi-condition equality query by atomic service code and dictionary type; returns 0 or 1 dictionary entity.
     * Queries the cache via secondary indexes first; on miss, queries the database and writes back.
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @param dictType dictionary type, non-blank
     * @return matching dictionary entity, or null when not found
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#dictType"],
        entityClass = SysDictCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType"]
    )
    open fun getDictByAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): SysDictCacheEntry? {
        return sysDictDao.fetchDictByAtomicServiceCodeAndDictType(atomicServiceCode, dictType)
    }

    // ---------- Full refresh ----------

    /**
     * Load all dictionaries from the database and refresh the Hash cache.
     *
     * @param clear when true, clear the current cache before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not active; skip loading dictionary Hash cache")
            return
        }
        val cache = hashCache()
        val list = sysDictDao.searchAs<SysDictCacheEntry>()
        log.debug("Loaded ${list.size} dictionaries from database; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("Dictionary Hash cache refresh completed")
    }

    // ---------- Post-write sync (called by business after insert/update/delete) ----------

    /**
     * Post-insert sync: load the entity for the given id from the database, write to cache, and build secondary indexes.
     *
     * @param id primary key of the inserted dictionary
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysDictDao.getAs<SysDictCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-insert sync (overload accepting a business object plus id). Behaves identically to [syncOnInsert(id)].
     *
     * @param any business object, used only to differentiate the overload
     * @param id primary key of the inserted dictionary
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Post-update sync: reload the entity for the given id from the database, write to cache, and update secondary indexes.
     *
     * @param id primary key of the updated dictionary
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysDictDao.getAs<SysDictCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Post-update sync (overload accepting previous atomicServiceCode, dictType, etc.). Behaves identically to [syncOnUpdate(id)].
     */
    open fun syncOnUpdate(any: Any, id: String, oldAtomicServiceCode: String?, oldDictType: String?) {
        syncOnUpdate(id)
    }

    /**
     * Post active-flag-update sync. Behaves identically to [syncOnUpdate(id)].
     *
     * @param id dictionary primary key
     * @param active new active flag
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        syncOnUpdate(id)
    }

    /**
     * Post-delete sync: remove the id from the cache and from the secondary property Set indexes.
     *
     * @param id primary key of the deleted dictionary
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysDictCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-batch-delete sync: remove the given ids from the cache and from the secondary property Set indexes.
     *
     * @param ids primary keys of the deleted dictionaries
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysDictCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    // region Event subscriptions (dispatched by SysDictService after transaction commit) -----------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictBatchDeleted): Unit = syncOnBatchDelete(event.ids)

    // endregion

    /**
     * Build a composite key for the "atomic service code + dictionary type" dimension; format: atomicServiceCode + delimiter + dictType.
     * Used by callers that need to follow the same cache key convention.
     */
    fun getKeyAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}"
    }
}
