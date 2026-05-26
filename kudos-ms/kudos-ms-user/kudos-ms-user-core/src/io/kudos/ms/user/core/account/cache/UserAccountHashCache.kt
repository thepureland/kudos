package io.kudos.ms.user.core.account.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache.Companion.CACHE_NAME
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAccountInserted
import io.kudos.ms.user.core.account.event.UserAccountUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * User Hash cache handler (consolidates the original UserByIdCache and UserIdByTenantIdAndUsernameCache logic).
 *
 * Source table: user_account; primary key is id. The cache key is id and the value is [UserAccountCacheEntry].
 * Stored as a Hash, supporting get-by-id and a secondary index by tenantId+username.
 *
 * - By id: getUserById, getUsersByIds (equivalent to the legacy UserByIdCache, including active=false)
 * - By tenant+username: getUsersByTenantIdAndUsername (equivalent to the legacy UserIdByTenantIdAndUsernameCache lookup)
 *
 * Before use, add an entry named [CACHE_NAME] with hash=true in the sys_cache config table.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class UserAccountHashCache : AbstractHashCacheHandler<UserAccountCacheEntry>() {

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "USER_ACCOUNT__HASH"

        /** Filterable secondary properties: secondary indexes on tenantId and username. */
        val FILTERABLE_PROPERTIES = setOf(
            UserAccountCacheEntry::tenantId.name,
            UserAccountCacheEntry::username.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = UserAccountCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): UserAccountCacheEntry? = userAccountDao.getAs(id.toString())

    /**
     * Get a user from the cache by id; on miss, load from DB and write back.
     *
     * @param id user id (primary key), non-blank
     * @return user cache entry, or null if not found
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = UserAccountCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "username"]
    )
    open fun getUserById(id: String): UserAccountCacheEntry? {
        require(id.isNotBlank()) { "id must not be blank when fetching a user" }
        return userAccountDao.getAs<UserAccountCacheEntry>(id)
    }

    /**
     * Batch-get users from the cache by multiple ids; missing entries are loaded from DB and written back.
     *
     * @param ids user id collection (may be empty)
     * @return id -> entity map, including only ids that were found
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = UserAccountCacheEntry::class,
        filterableProperties = ["tenantId", "username"]
    )
    open fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = userAccountDao.getByIdsAs<UserAccountCacheEntry>(ids)
        return list.filter { it.id.isNotBlank() && it.id in ids }.associateBy { it.id }
    }

    /**
     * Get a user from the cache by tenant id and username; on miss, load from DB and write back.
     *
     * @param tenantId tenant id
     * @param username username
     * @return user cache entry, or null if not found
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#username"],
        entityClass = UserAccountCacheEntry::class,
        filterableProperties = ["tenantId", "username"]
    )
    open fun getUsersByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry? {
        return userAccountDao.getUsersByTenantIdAndUsername(tenantId, username)
    }

    /**
     * Load all users from DB and refresh the Hash cache (including active=false, equivalent to the legacy UserByIdCache full load).
     *
     * @param clear when true, clear the cache before writing; when false, overwrite in place
     */
    override fun reloadAll(clear: Boolean) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; skipping user Hash cache load")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = userAccountDao.searchAs<UserAccountCacheEntry>()
        log.debug("Loaded ${list.size} users from DB; refreshing Hash cache")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after user insert: load the entity by id from DB and write it to the cache. */
    open fun syncOnInsert(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || !HashCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = userAccountDao.getAs<UserAccountCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after user update: reload from DB and write to the cache. */
    open fun syncOnUpdate(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        val item = userAccountDao.getAs<UserAccountCacheEntry>(id) ?: return
        if (HashCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** Sync after user delete: remove the id from the cache. */
    open fun syncOnDelete(id: String) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, UserAccountCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** Sync after batch user delete: remove the ids from the cache (single Pub/Sub notification to avoid N+1 storm). */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!HashCacheKit.isCacheActive(CACHE_NAME) || ids.isEmpty()) return
        hashCache().deleteByIds(CACHE_NAME, ids, UserAccountCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted): Unit = syncOnBatchDelete(event.ids)
}
