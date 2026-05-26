package io.kudos.ms.sys.core.i18n.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.i18n.dao.SysI18nDao
import io.kudos.ms.sys.core.i18n.event.SysI18nBatchDeleted
import io.kudos.ms.sys.core.i18n.event.SysI18nDeleted
import io.kudos.ms.sys.core.i18n.event.SysI18nInserted
import io.kudos.ms.sys.core.i18n.event.SysI18nUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Unified i18n cache handler. Stores [SysI18nCacheEntry] using a Hash structure.
 *
 * Supports querying and write-back by primary and secondary properties:
 *  1. By primary key id: single [getI18nById], batch [getI18nsByIds]
 *  2. By secondary properties: locale + atomicServiceCode + i18nTypeDictCode + namespace
 *
 * Source table: sys_i18n
 *
 * Uses the secondary properties in [FILTERABLE_PROPERTIES] to build Set indexes supporting multi-condition equality queries. All writes, deletes, and full refreshes must use the same secondary property set to keep indexes consistent.
 *
 * Before use, add a configuration named [CACHE_NAME] with hash=true in the sys_cache configuration table.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysI18nHashCache : AbstractHashCacheHandler<SysI18nCacheEntry>() {

    @Resource
    private lateinit var sysI18nDao: SysI18nDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_I18N__HASH"

        /** Filterable secondary properties, used to build secondary indexes by locale / atomicServiceCode / i18nTypeDictCode / namespace. */
        val FILTERABLE_PROPERTIES = setOf(
            SysI18nCacheEntry::locale.name,
            SysI18nCacheEntry::atomicServiceCode.name,
            SysI18nCacheEntry::i18nTypeDictCode.name,
            SysI18nCacheEntry::namespace.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysI18nCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysI18nCacheEntry? =
        sysI18nDao.get(id.toString(), SysI18nCacheEntry::class)

    // ---------- By primary key id ----------

    /**
     * Get an i18n entry from the cache by primary key id. On miss, load from DB and write back.
     *
     * @param id i18n primary key, non-blank
     * @return i18n cache entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysI18nCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["locale", "atomicServiceCode", "i18nTypeDictCode", "namespace"]
    )
    open fun getI18nById(id: String): SysI18nCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching i18n" }
        return sysI18nDao.get(id, SysI18nCacheEntry::class)
    }

    /**
     * Batch fetch i18n entries from the cache by multiple primary key ids. On miss, load from DB and write back.
     *
     * @param ids i18n primary key collection, may be empty
     * @return id -> cache object map, containing only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysI18nCacheEntry::class,
        filterableProperties = ["locale", "atomicServiceCode", "i18nTypeDictCode", "namespace"]
    )
    open fun getI18nsByIds(ids: List<String>): Map<String, SysI18nCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysI18nDao.getByIdsAs<SysI18nCacheEntry>(ids)
        return list.mapNotNull { e ->
            val id = e.id ?: return@mapNotNull null
            if (id.isNotBlank() && id in ids) id to e else null
        }.toMap()
    }

    // ---------- By locale + atomicServiceCode + i18nTypeDictCode + namespace ----------

    /**
     * Multi-condition equality query by locale, atomic service code, i18n type, and namespace. Returns the list of matched, enabled i18n entries.
     * Queries the cache via secondary indexes first; on miss, loads from DB and writes back.
     * namespace may be omitted or empty; when empty, the query uses locale + atomicServiceCode + i18nTypeDictCode (namespace is not used as a filter).
     *
     * @param locale language_region, non-blank
     * @param atomicServiceCode atomic service code, non-blank
     * @param i18nTypeDictCode i18n type dictionary code, non-blank
     * @param namespace namespace, defaults to null; when blank it is not used in the query
     * @return matched cache entries
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#locale", "#atomicServiceCode", "#i18nTypeDictCode", "#namespace"],
        entityClass = SysI18nCacheEntry::class,
        filterableProperties = ["locale", "atomicServiceCode", "i18nTypeDictCode", "namespace"]
    )
    open fun getI18ns(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String? = null
    ): List<SysI18nCacheEntry> {
        require(locale.isNotBlank()) { "locale must not be blank when fetching i18n" }
        require(atomicServiceCode.isNotBlank()) { "atomicServiceCode must not be blank when fetching i18n" }
        require(i18nTypeDictCode.isNotBlank()) { "i18nTypeDictCode must not be blank when fetching i18n" }
        return sysI18nDao.fetchActiveI18nsForCache(locale, atomicServiceCode, i18nTypeDictCode, namespace ?: "")
    }

    /**
     * Multi-condition equality query by locale, atomic service code, i18n type, and namespace. Returns a map of matched, enabled i18n entries.
     * Queries the cache via secondary indexes first; on miss, loads from DB and writes back.
     *
     * @param locale language-region, non-blank
     * @param atomicServiceCode atomic service code, non-blank
     * @param i18nTypeDictCode i18n type dictionary code, non-blank
     * @param namespace namespace
     * @return Map<i18n key, translated text>
     */
    open fun getI18nMap(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String
    ): Map<String, String> {
        val items = getSelf<SysI18nHashCache>().getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        return items.associateBy({ it.key }, { it.value })
    }

    /**
     * Multi-condition equality query by locale, atomic service code, and i18n type. Returns a map of matched, enabled i18n entries.
     * Queries the cache via secondary indexes first; on miss, loads from DB and writes back.
     *
     * @param locale language-region, non-blank
     * @param atomicServiceCode atomic service code, non-blank
     * @param i18nTypeDictCode i18n type dictionary code, non-blank
     * @return Map<namespace, Map<i18n key, translated text>>
     */
    open fun getI18nMap(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
    ): Map<String, Map<String, String>> {
        val items = getSelf<SysI18nHashCache>().getI18ns(locale, atomicServiceCode, i18nTypeDictCode)
        return items.groupBy { it.namespace }
            .mapValues { (_, values) ->
                values.associate { it.key to it.value }
            }
    }

    // ---------- Full refresh and sync ----------

    /**
     * Load all enabled i18n entries from DB and refresh the Hash cache.
     *
     * @param clear when true, clear before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skipping i18n Hash cache load")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysI18nDao.fetchAllActiveI18nsForCache()
        log.debug("Loaded ${list.size} i18n entries from the database; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Sync after i18n insert: load the entity with the given id from DB and write it into the cache.
     *
     * @param id primary key
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysI18nDao.get(id, SysI18nCacheEntry::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Sync after i18n insert (overload, accepts business object and id).
     *
     * @param any business object, used only to distinguish the overload
     * @param id primary key
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Sync after i18n update: reload the entity with this id from DB and write back into the cache.
     *
     * @param id primary key
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysI18nDao.get(id, SysI18nCacheEntry::class) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Sync after i18n update (overload).
     *
     * @param any business object, used only to distinguish the overload
     * @param id primary key
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * Sync after i18n delete: remove the id and its secondary-property indexes from the cache.
     *
     * @param id primary key
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysI18nCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Sync after batch delete: remove these ids and their secondary-property indexes from the cache.
     *
     * @param ids primary key collection
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        log.debug("After batch deleting sys_i18n ids $ids, evicting from ${cacheName()} cache...")
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysI18nCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
        log.debug("${cacheName()} cache sync complete.")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysI18nInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysI18nUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysI18nDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysI18nBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
