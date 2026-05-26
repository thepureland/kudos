package io.kudos.ms.auth.core.group.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.group.vo.AuthGroupCacheEntry
import io.kudos.ms.auth.core.group.cache.AuthGroupHashCache.Companion.CACHE_NAME
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupInserted
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * User group Hash cache handler.
 *
 * Source table: auth_group. The primary key is id, the cache key is the id,
 * and the value is [AuthGroupCacheEntry]. Stored as a Hash that supports lookups
 * by id and by a (tenantId, code) secondary index.
 *
 * - By id: getGroupById, getGroupsByIds
 * - By tenant + group code: getGroupByTenantIdAndGroupCode
 *
 * Requires a sys_cache entry named [CACHE_NAME] with hash=true.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AuthGroupHashCache : AbstractHashCacheHandler<AuthGroupCacheEntry>() {

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "AUTH_GROUP__HASH"

        /** Filterable secondary properties: builds a secondary index over (tenantId, code); excludes active. */
        val FILTERABLE_PROPERTIES = setOf(
            AuthGroupCacheEntry::tenantId.name,
            AuthGroupCacheEntry::code.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = AuthGroupCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): AuthGroupCacheEntry? = authGroupDao.getAs(id.toString())

    /**
     * Loads a user group from the cache by id; on a miss, reads from the DB and writes back.
     *
     * @param id user group id (primary key); must not be blank
     * @return cache entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = AuthGroupCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "code"]
    )
    open fun getGroupById(id: String): AuthGroupCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching a user group" }
        return authGroupDao.getAs<AuthGroupCacheEntry>(id)
    }

    /**
     * Batch-loads user groups from the cache by id; misses are read from the DB and written back.
     *
     * @param ids user group ids; may be empty
     * @return id -> entity map, containing only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = AuthGroupCacheEntry::class,
        filterableProperties = ["tenantId", "code"]
    )
    open fun getGroupsByIds(ids: Collection<String>): Map<String, AuthGroupCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = authGroupDao.getByIdsAs<AuthGroupCacheEntry>(ids)
        return list.filter { it.id.isNotBlank() && it.id in ids }.associateBy { it.id }
    }

    /**
     * Get a group from the cache by tenant id and group code; on miss, load from DB and write back (regardless of active).
     *
     * @param tenantId tenant id
     * @param code group code
     * @return group cache entry, or null if not found
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#code"],
        entityClass = AuthGroupCacheEntry::class,
        filterableProperties = ["tenantId", "code"]
    )
    open fun getGroupByTenantIdAndGroupCode(tenantId: String, code: String): AuthGroupCacheEntry? {
        return authGroupDao.searchGroupByTenantIdAndGroupCode(tenantId, code)
    }

    /**
     * Load all groups from DB and refresh the Hash cache (including active=false, equivalent to the legacy GroupByIdCache full load).
     *
     * @param clear when true, clear the cache before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; skipping group Hash cache load")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = authGroupDao.searchAs<AuthGroupCacheEntry>()
        log.debug("Loaded ${list.size} groups from DB; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after group insert: load the entity by id from DB and write it to the cache. */
    open fun syncOnInsert(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || !HashCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = authGroupDao.getAs<AuthGroupCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after group update: reload from DB and write to the cache. */
    open fun syncOnUpdate(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        val item = authGroupDao.getAs<AuthGroupCacheEntry>(id) ?: return
        if (HashCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** Sync after group delete: remove the id from the cache. */
    open fun syncOnDelete(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, AuthGroupCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after batch group delete: remove the ids from the cache (single Pub/Sub notification to avoid N+1 storm). */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || ids.isEmpty()) return
        hashCache().deleteByIds(CACHE_NAME, ids, AuthGroupCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
