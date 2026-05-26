package io.kudos.ms.sys.core.system.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.dao.SysSystemDao
import io.kudos.ms.sys.core.system.event.SysSystemBatchDeleted
import io.kudos.ms.sys.core.system.event.SysSystemDeleted
import io.kudos.ms.sys.core.system.event.SysSystemInserted
import io.kudos.ms.sys.core.system.event.SysSystemUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Hash cache handler for systems (keyed by code).
 *
 * Source table: sys_system; primary key is code; the cache key is the code and the value is [SysSystemCacheEntry].
 * Stored using a Hash structure; accessed by code via [HashCacheableByPrimary] / [HashBatchCacheableByPrimary].
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysSystemHashCache : AbstractHashCacheHandler<SysSystemCacheEntry>() {

    @Resource
    private lateinit var sysSystemDao: SysSystemDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_SYSTEM__HASH"

        /** Filterable secondary properties: secondary index on subSystem for sub-system flag queries. */
        val FILTERABLE_PROPERTIES = setOf(SysSystemCacheEntry::subSystem.name)
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysSystemCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysSystemCacheEntry? = sysSystemDao.getAs(id.toString())

    /**
     * Gets a system from cache by code; on miss, queries the database and writes back.
     *
     * @param code system code (primary key), non-blank
     * @return cached system entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#code",
        entityClass = SysSystemCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["subSystem"]
    )
    open fun getSystemByCode(code: String): SysSystemCacheEntry? {
        require(code.isNotBlank()) { "code must not be blank when fetching a system" }
        return sysSystemDao.getAs<SysSystemCacheEntry>(code)
    }

    /**
     * Batch gets systems from cache by codes; misses are loaded from the database and written back.
     *
     * @param codes system code collection, may be empty
     * @return code -> entity map; only contains codes that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysSystemCacheEntry::class,
        filterableProperties = ["subSystem"]
    )
    open fun getSystemsByCodes(codes: List<String>): Map<String, SysSystemCacheEntry> {
        if (codes.isEmpty()) return emptyMap()
        val list = sysSystemDao.getByIdsAs<SysSystemCacheEntry>(codes)
        return list.filter { it.id.isNotBlank() && it.id in codes }.associateBy { it.id }
    }

    /**
     * Gets the system list from cache by sub-system flag; on miss, queries the database and writes back.
     *
     * @param subSystem true to fetch only sub-systems, false to fetch only non-sub-systems
     * @return list of cached system entries
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#subSystem"],
        entityClass = SysSystemCacheEntry::class,
        filterableProperties = ["subSystem"]
    )
    open fun getSystemsByType(subSystem: Boolean): List<SysSystemCacheEntry> {
        return sysSystemDao.fetchSystemsByType(subSystem)
    }

    /** Returns all sub-systems (subSystem=true). */
    open fun listSubSystems(): List<SysSystemCacheEntry> = getSystemsByType(true)

    /**
     * Returns all systems (from cache); if the cache is empty, loads from the database, writes back, and returns.
     *
     * @return list of cached system entries; queries directly from the database when caching is disabled
     */
    open fun getAllSystems(): List<SysSystemCacheEntry> {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            return sysSystemDao.searchAs<SysSystemCacheEntry>()
        }
        val cache = hashCache()
        var list = cache.listAll(CACHE_NAME, SysSystemCacheEntry::class)
        if (list.isEmpty()) {
            reloadAll(clear = false)
            list = cache.listAll(CACHE_NAME, SysSystemCacheEntry::class)
        }
        return list
    }

    /**
     * Loads all systems from the database and refreshes the Hash cache.
     *
     * @param clear when true, clears before writing; when false, overwrites in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skip loading system Hash cache.")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysSystemDao.searchAs<SysSystemCacheEntry>()
        log.debug("Loaded ${list.size} systems from database; refreshing Hash cache.")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Syncs after system insert: loads the entity by code from the database and writes it to the cache. */
    open fun syncOnInsert(code: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysSystemDao.getAs<SysSystemCacheEntry>(code) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Syncs after system insert (overload accepting a business object and code). Same behavior as [syncOnInsert].
     *
     * @param any business object, only used to distinguish the overload
     * @param code system code (primary key)
     */
    open fun syncOnInsert(any: Any, code: String) {
        syncOnInsert(code)
    }

    /** Syncs after system update: reloads the entity from the database and writes it to the cache. */
    open fun syncOnUpdate(code: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysSystemDao.getAs<SysSystemCacheEntry>(code) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Syncs after system update (overload with business object). Same behavior as [syncOnUpdate].
     *
     * @param any business object
     * @param code system code (primary key)
     */
    open fun syncOnUpdate(any: Any, code: String) {
        syncOnUpdate(code)
    }

    /** Syncs after system delete: removes the code from the cache. */
    open fun syncOnDelete(code: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, code, SysSystemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Syncs after batch system delete: removes the given codes from the cache. */
    open fun syncOnBatchDelete(codes: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        codes.forEach { cache.deleteById(CACHE_NAME, it, SysSystemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysSystemInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysSystemUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysSystemDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysSystemBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
