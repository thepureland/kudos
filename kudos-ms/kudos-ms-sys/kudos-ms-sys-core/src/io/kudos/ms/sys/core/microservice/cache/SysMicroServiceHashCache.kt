package io.kudos.ms.sys.core.microservice.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.cache.SysMicroServiceHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.microservice.dao.SysMicroServiceDao
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceBatchDeleted
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceDeleted
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceInserted
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Hash cache handler for microservices (keyed by code).
 *
 * Source table: sys_micro_service; primary key is code, the cache key is the code, and the value is [SysMicroServiceCacheEntry].
 * Stored using a Hash structure, accessed by code through [HashCacheableByPrimary] / [HashBatchCacheableByPrimary].
 *
 * Before use, add a configuration named [CACHE_NAME] with hash=true in the sys_cache configuration table.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysMicroServiceHashCache : AbstractHashCacheHandler<SysMicroServiceCacheEntry>() {

    @Resource
    private lateinit var sysMicroServiceDao: SysMicroServiceDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_MICRO_SERVICE__HASH"

        /** Filterable secondary properties: build a secondary index by atomicService for querying by whether it is an atomic service. */
        val FILTERABLE_PROPERTIES = setOf(SysMicroServiceCacheEntry::atomicService.name)
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysMicroServiceCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysMicroServiceCacheEntry? = sysMicroServiceDao.getAs(id.toString())

    /**
     * Get a microservice from the cache by code. On miss, load from DB and write back.
     *
     * @param code microservice code (primary key), non-blank
     * @return microservice cache entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#code",
        entityClass = SysMicroServiceCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["atomicService"]
    )
    open fun getMicroServiceByCode(code: String): SysMicroServiceCacheEntry? {
        require(code.isNotBlank()) { "code must not be blank when fetching microservice" }
        return sysMicroServiceDao.get(code, SysMicroServiceCacheEntry::class)
    }

    /**
     * Batch fetch microservices from the cache by multiple codes. On miss, load from DB and write back.
     *
     * @param codes collection of microservice codes, may be empty
     * @return code -> entity map, containing only codes that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysMicroServiceCacheEntry::class,
        filterableProperties = ["atomicService"]
    )
    open fun getMicroServicesByCodes(codes: List<String>): Map<String, SysMicroServiceCacheEntry> {
        if (codes.isEmpty()) return emptyMap()
        val list = sysMicroServiceDao.getByIdsAs<SysMicroServiceCacheEntry>(codes)
        return list.filter { it.id.isNotBlank() && it.id in codes }.associateBy { it.id }
    }

    /**
     * Get the list of microservices from the cache by whether they are atomic services. On miss, load from DB and write back.
     *
     * @param atomicService true to query only atomic services, false to query only non-atomic services
     * @return list of microservice cache entries
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicService"],
        entityClass = SysMicroServiceCacheEntry::class,
        filterableProperties = ["atomicService"]
    )
    open fun getMicroServicesByType(atomicService: Boolean): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceDao.fetchMicroServiceByTypeForCache(atomicService)
    }

    /** Get the list of all atomic services (atomicService=true). */
    open fun listAtomicServices(): List<SysMicroServiceCacheEntry> = getMicroServicesByType(true)

    /**
     * Get all microservices; if the cache is empty, load from DB and write back, then return.
     *
     * @return list of microservice cache entries; when the cache is disabled, queries directly from the DB
     */
    open fun getAllMicroServices(): List<SysMicroServiceCacheEntry> {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            return sysMicroServiceDao.searchAs<SysMicroServiceCacheEntry>()
        }
        val cache = hashCache()
        var list = cache.listAll(CACHE_NAME, SysMicroServiceCacheEntry::class)
        if (list.isEmpty()) {
            reloadAll(clear = false)
            list = cache.listAll(CACHE_NAME, SysMicroServiceCacheEntry::class)
        }
        return list
    }

    /**
     * Load all microservices from DB and refresh the Hash cache.
     *
     * @param clear when true, clear before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skipping microservice Hash cache load")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysMicroServiceDao.searchAs<SysMicroServiceCacheEntry>()
        log.debug("Loaded ${list.size} microservices from the database; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after microservice insert: load the entity with the given code from DB and write it into the cache. */
    open fun syncOnInsert(code: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysMicroServiceDao.get(code, SysMicroServiceCacheEntry::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * Sync after microservice insert (overload taking business object and code). Behaves like [syncOnInsert].
     *
     * @param any business object, used only to differentiate the overload
     * @param code microservice code (primary key)
     */
    open fun syncOnInsert(any: Any, code: String) {
        syncOnInsert(code)
    }

    /** Sync after microservice update: reload from DB and write into the cache. */
    open fun syncOnUpdate(code: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysMicroServiceDao.get(code, SysMicroServiceCacheEntry::class) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * Sync after microservice update (overload taking business object). Behaves like [syncOnUpdate].
     *
     * @param any business object
     * @param code microservice code (primary key)
     */
    open fun syncOnUpdate(any: Any, code: String) {
        syncOnUpdate(code)
    }

    /** Sync after microservice delete: remove the code from the cache. */
    open fun syncOnDelete(code: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, code, SysMicroServiceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after batch delete of microservices: remove these codes from the cache. */
    open fun syncOnBatchDelete(codes: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        codes.forEach { cache.deleteById(CACHE_NAME, it, SysMicroServiceCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysMicroServiceInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysMicroServiceUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysMicroServiceDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysMicroServiceBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
