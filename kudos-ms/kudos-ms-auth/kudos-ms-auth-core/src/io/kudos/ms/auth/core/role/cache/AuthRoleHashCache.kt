package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache.Companion.CACHE_NAME
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleInserted
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Role hash-cache handler.
 *
 * Source table: auth_role; primary key is id, the cache key is the id, the value is [AuthRoleCacheEntry].
 * Stored as a Hash, supports get-by-id access and tenantId+code secondary-index queries.
 *
 * - By id: getRoleById, getRolesByIds
 * - By tenant + role code: getRoleByTenantIdAndRoleCode
 *
 * Before use, add a config row named [CACHE_NAME] with hash=true to the sys_cache configuration table.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AuthRoleHashCache : AbstractHashCacheHandler<AuthRoleCacheEntry>() {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "AUTH_ROLE__HASH"

        /** Filterable secondary attributes: secondary indexes on tenantId and code (active excluded) */
        val FILTERABLE_PROPERTIES = setOf(
            AuthRoleCacheEntry::tenantId.name,
            AuthRoleCacheEntry::code.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = AuthRoleCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): AuthRoleCacheEntry? = authRoleDao.getAs(id.toString())

    /**
     * Returns a role by id from the cache; falls through to the DB and writes back on a miss.
     *
     * @param id role id (primary key), non-blank
     * @return cache entry, or null when not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = AuthRoleCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "code"]
    )
    open fun getRoleById(id: String): AuthRoleCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching a role" }
        return authRoleDao.getAs<AuthRoleCacheEntry>(id)
    }

    /**
     * Batch-fetches roles by ids from the cache; loads misses from the DB and writes them back.
     *
     * @param ids collection of role ids, may be empty
     * @return id -> entity map, containing only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = AuthRoleCacheEntry::class,
        filterableProperties = ["tenantId", "code"]
    )
    open fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = authRoleDao.getByIdsAs<AuthRoleCacheEntry>(ids)
        return list.filter { it.id.isNotBlank() && it.id in ids }.associateBy { it.id }
    }

    /**
     * Returns a role by tenant and role code from the cache; falls through to the DB and writes back
     * on a miss (regardless of active state).
     *
     * @param tenantId tenant id
     * @param code role code
     * @return cache entry, or null when not found
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#code"],
        entityClass = AuthRoleCacheEntry::class,
        filterableProperties = ["tenantId", "code"]
    )
    open fun getRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheEntry? {
        return authRoleDao.searchRoleByTenantIdAndRoleCode(tenantId, code)
    }

    /**
     * Fully loads roles from the DB and refreshes the hash cache (includes active=false; equivalent
     * to the full reload in the legacy RoleByIdCache).
     *
     * @param clear when true, clear first then write; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache not enabled; skip loading role hash cache")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = authRoleDao.searchAs<AuthRoleCacheEntry>()
        log.debug("Loaded ${list.size} roles from DB; refreshing hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Post-insert sync: loads the entity by id from the DB and writes it into the cache. */
    open fun syncOnInsert(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || !HashCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = authRoleDao.getAs<AuthRoleCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Post-update sync: reloads from the DB and writes into the cache. */
    open fun syncOnUpdate(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        val item = authRoleDao.getAs<AuthRoleCacheEntry>(id) ?: return
        if (HashCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** Post-delete sync: removes the id from the cache. */
    open fun syncOnDelete(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, AuthRoleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Post-batch-delete sync: removes the given ids from the cache (single Pub/Sub message, avoids N+1 storm). */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || ids.isEmpty()) return
        hashCache().deleteByIds(CACHE_NAME, ids, AuthRoleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
