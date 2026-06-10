package io.kudos.ms.user.core.org.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.org.cache.UserOrgHashCache.Companion.CACHE_NAME
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgInserted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Organization Hash cache handler.
 *
 * Source table: user_org; primary key is id. The cache key is id and the value is [UserOrgCacheEntry].
 * Stored as a Hash, supporting get-by-id and a secondary index by tenantId.
 *
 * - By id: getOrgById, getOrgsByIds (equivalent to the legacy OrgByIdCache, including active=false)
 * - By tenant: getOrgsByTenantId, getOrgIdsByTenantId
 *
 * Before use, add an entry named [CACHE_NAME] with hash=true in the sys_cache config table.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class UserOrgHashCache : AbstractHashCacheHandler<UserOrgCacheEntry>() {

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "USER_ORG__HASH"

        /** Filterable secondary properties: indexed by tenantId for tenant-scoped queries. */
        val FILTERABLE_PROPERTIES = setOf(
            UserOrgCacheEntry::tenantId.name,
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = UserOrgCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): UserOrgCacheEntry? = userOrgDao.getAs(id.toString())

    /**
     * Get an organization from the cache by id; on miss, load from DB and write back.
     *
     * @param id organization id (primary key), non-blank
     * @return cache entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = UserOrgCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["tenantId"]
    )
    open fun getOrgById(id: String): UserOrgCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching an organization" }
        return userOrgDao.getAs<UserOrgCacheEntry>(id)
    }

    /**
     * Batch-get organizations from the cache by multiple ids; missing entries are loaded from DB and written back.
     *
     * @param ids organization id collection (may be empty)
     * @return id -> entity map, including only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = UserOrgCacheEntry::class,
        filterableProperties = ["tenantId"]
    )
    open fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = userOrgDao.getByIdsAs<UserOrgCacheEntry>(ids)
        return list.filter { it.id.isNotBlank() && it.id in ids }.associateBy { it.id }
    }

    /**
     * Get the organization list for a tenant from the cache; on miss, load from DB and write back.
     *
     * @param tenantId tenant id
     * @return list of organization cache entries
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        // Note: must only reference real method parameters. The previous ["#tenantId", "#active"] referenced
        // a non-existent #active parameter, which evaluated to null in SpEL and caused the aspect to silently
        // bypass the cache on every call (and "active" is not in filterableProperties either).
        filterExpressions = ["#tenantId"],
        entityClass = UserOrgCacheEntry::class,
        filterableProperties = ["tenantId"]
    )
    open fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheEntry> {
        return userOrgDao.searchOrgsByTenantIdForCache(tenantId)
    }

    /**
     * Load all organizations from DB and refresh the Hash cache (including active=false, equivalent to the legacy OrgByIdCache full load).
     *
     * @param clear when true, clear the cache before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; skipping organization Hash cache load")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = userOrgDao.searchAs<UserOrgCacheEntry>()
        log.debug("Loaded ${list.size} organizations from DB; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after organization insert: load the entity by id from DB and write it to the cache. */
    open fun syncOnInsert(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || !HashCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = userOrgDao.getAs<UserOrgCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after organization update: reload from DB and write to the cache. */
    open fun syncOnUpdate(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        val item = userOrgDao.getAs<UserOrgCacheEntry>(id) ?: return
        if (HashCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** Sync after organization delete: remove the id from the cache. */
    open fun syncOnDelete(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, UserOrgCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after batch organization delete: remove the ids from the cache (single Pub/Sub notification to avoid N+1 storm). */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || ids.isEmpty()) return
        hashCache().deleteByIds(CACHE_NAME, ids, UserOrgCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
