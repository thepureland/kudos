package io.kudos.ms.sys.core.dict.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dict.dao.VSysDictItemDao
import io.kudos.ms.sys.core.dict.event.SysDictItemBatchDeleted
import io.kudos.ms.sys.core.dict.event.SysDictItemDeleted
import io.kudos.ms.sys.core.dict.event.SysDictItemInserted
import io.kudos.ms.sys.core.dict.event.SysDictItemUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Unified dictionary item cache handler, storing [SysDictItemCacheEntry] in a Hash structure.
 *
 * Data source: view v_sys_dict_item (sys_dict_item left join sys_dict).
 *
 * Provides read and write-back capabilities by primary and secondary properties:
 * - **By primary key id**: single and batch.
 * - **By secondary properties**: atomicServiceCode + dictType + itemCode (single); atomicServiceCode + dictType (list); parentId (child item list).
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
open class SysDictItemHashCache : AbstractHashCacheHandler<SysDictItemCacheEntry>() {

    @Resource
    private lateinit var vSysDictItemDao: VSysDictItemDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_DICT_ITEM__HASH"

        /** Secondary property names used for equality filtering and Set indexes; writes/deletes/full refreshes must match this set */
        val FILTERABLE_PROPERTIES = setOf(
            SysDictItemCacheEntry::atomicServiceCode.name,
            SysDictItemCacheEntry::dictType.name,
            SysDictItemCacheEntry::itemCode.name,
            SysDictItemCacheEntry::parentId.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysDictItemCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysDictItemCacheEntry? = vSysDictItemDao.getAs<SysDictItemCacheEntry>(id.toString())

    // ---------- 1. By primary key id ----------

    /**
     * Get a single dictionary item entity by primary key id.
     * Queries the cache first; on miss, queries the view and writes back, building secondary indexes per [FILTERABLE_PROPERTIES].
     *
     * @param id dictionary item primary key, non-blank
     * @return the cache entry, or null when not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysDictItemCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItemById(id: String): SysDictItemCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching dictionary item" }
        return vSysDictItemDao.getAs<SysDictItemCacheEntry>(id)
    }

    /**
     * Batch get dictionary item entities by primary key id list.
     * Queries the cache first; for any missing ids, queries the view and writes back, building secondary indexes per [FILTERABLE_PROPERTIES].
     *
     * @param ids list of dictionary item primary keys, may be empty
     * @return id -> entity map, only including found ids
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItemsByIds(ids: Set<String>): Map<String, SysDictItemCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = vSysDictItemDao.getByIdsAs<SysDictItemCacheEntry>(ids).map { it }
        val byId = list.associateBy { it.id.trim() }
        return ids.mapNotNull { id ->
            val key = id.trim()
            byId[key]?.let { key to it }
        }.toMap()
    }

    // ---------- 2. By atomicServiceCode + dictType + itemCode ----------

    /**
     * Multi-condition equality query by atomic service code, dictionary type, item code and active flag; returns 0 or 1 dictionary item.
     * Queries the cache via secondary indexes first; on miss, queries the view and writes back.
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @param dictType dictionary type, non-blank
     * @param itemCode dictionary item code, non-blank
     * @return matching dictionary item entity, or null when not found
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#dictType", "#itemCode"],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItem(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry? {
        return vSysDictItemDao.fetchByAtomicServiceCodeAndDictTypeAndItemCode(
            atomicServiceCode,
            dictType,
            itemCode
        )
    }

    // ---------- 3. By atomicServiceCode + dictType ----------

    /**
     * Multi-condition equality query by atomic service code, dictionary type and active flag; returns matching dictionary item list sorted by orderNum.
     * Queries the cache via secondary indexes first; on miss, queries the view and writes back.
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @param dictType dictionary type, non-blank
     * @return list of matching dictionary item entities
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#dictType"],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItems(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry> {
        return vSysDictItemDao.searchByAtomicServiceCodeAndDictType(atomicServiceCode, dictType).map { it }
    }

    // ---------- 4. By parentId ----------

    /**
     * Query child dictionary item list by parent dictionary item id and active flag, sorted by orderNum.
     * Queries the cache via secondary indexes first; on miss, queries the view and writes back.
     *
     * @param parentId parent dictionary item id, non-blank
     * @return list of matching dictionary item entities
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#parentId"],
        entityClass = SysDictItemCacheEntry::class,
        filterableProperties = ["atomicServiceCode", "dictType", "itemCode", "parentId"]
    )
    open fun getDictItems(parentId: String): List<SysDictItemCacheEntry> {
        require(parentId.isNotBlank()) { "parentId must not be blank when fetching child dictionary items" }
        return vSysDictItemDao.searchByParentId(parentId).map { it }
    }

    // ---------- Full refresh ----------

    /**
     * Load all dictionary items from the view and refresh the Hash cache.
     *
     * @param clear when true, clear the current cache before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not active; skip loading dictionary item Hash cache")
            return
        }
        val cache = hashCache()
        val list = vSysDictItemDao.searchAs<SysDictItemCacheEntry>().map { it }
        log.debug("Loaded ${list.size} dictionary items from view v_sys_dict_item; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("Dictionary item Hash cache refresh completed")
    }

    // ---------- Post-write sync (called by business after sys_dict_item insert/update/delete) ----------

    /**
     * Post-insert sync: load the entity for the given id from the view, write to cache, and build secondary indexes.
     *
     * @param id primary key of the inserted dictionary item
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = vSysDictItemDao.getAs<SysDictItemCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-insert sync (overload accepting a business object plus id). Behaves identically to [syncOnInsert(id)].
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Post-update sync: reload the entity for the given id from the view, write to cache, and update secondary indexes.
     *
     * @param id primary key of the updated dictionary item
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = vSysDictItemDao.getAs<SysDictItemCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Post-update sync (overload accepting previous secondary property values). Behaves identically to [syncOnUpdate(id)].
     */
    open fun syncOnUpdate(
        any: Any,
        id: String,
        oldAtomicServiceCode: String?,
        oldDictType: String?,
        oldItemCode: String?
    ) {
        syncOnUpdate(id)
    }

    /**
     * Post-delete sync: remove the id from the cache and from the secondary property Set indexes.
     *
     * @param id primary key of the deleted dictionary item
     * @param atomicServiceCode atomic service code the item belonged to (used for index removal)
     * @param dictType dictionary type (used for index removal), nullable
     * @param itemCode dictionary item code (used for index removal), nullable
     */
    open fun syncOnDelete(id: String, atomicServiceCode: String, dictType: String?, itemCode: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysDictItemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Post-batch-delete sync: remove the given ids from the cache and from the secondary property Set indexes.
     *
     * @param ids primary keys of the deleted dictionary items
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysDictItemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    // region Event subscriptions (dispatched by SysDictItemService after transaction commit) -----------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictItemInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictItemUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictItemDeleted): Unit =
        syncOnDelete(event.id, event.atomicServiceCode, event.dictType, event.itemCode)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDictItemBatchDeleted): Unit = syncOnBatchDelete(event.ids)

    // endregion

    /**
     * Build a composite key for the "atomic service code + dictionary type + dictionary item code" dimension.
     * Used by callers that need to follow the same cache key convention.
     */
    fun getKeyAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${itemCode}"
    }

    /**
     * Build a composite key for the "atomic service code + dictionary type" dimension.
     */
    fun getKeyAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): String {
        return "${atomicServiceCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${dictType}"
    }
}
