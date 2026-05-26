package io.kudos.ms.auth.core.group.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the list of group ids keyed by user id.
 *
 * 1. Source table: auth_group_user
 * 2. Caches the list of group ids that each user belongs to
 * 3. Cache key: userId
 * 4. Cache value: list of group ids (List<String>)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class GroupIdsByUserIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "AUTH_GROUP_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<GroupIdsByUserIdCache>().getGroupIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; not loading or caching group ids for all users!")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToGroupIdsMap = authGroupUserDao.searchAllUserIdToGroupIdsForCache()

        log.debug("Loaded ${users.size} users and ${userIdToGroupIdsMap.size} group-user groupings from DB.")

        // Clear the cache.
        if (clear) {
            clear()
        }

        // Cache the group id lists.
        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val groupIds = userIdToGroupIdsMap[userId] ?: emptyList()
            if (groupIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, userId, groupIds)
                log.debug("Cached ${groupIds.size} group ids for user ${userId}.")
            }
        }
    }

    /**
     * Get all group ids a user belongs to keyed by user id; on cache miss, load from DB and write back.
     *
     * @param userId user id
     * @return list of group ids
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getGroupIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Group ids for user ${userId} not in cache; loading from DB...")
        }

        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        log.debug("Loaded ${groupIds.size} group ids from DB for user ${userId}.")
        return groupIds.toList()
    }

    /**
     * Sync the cache after a user-group association changes.
     *
     * @param userId user id
     */
    open fun syncOnGroupUserChange(userId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After user ${userId} group associations changed, syncing ${CACHE_NAME} cache...")
            evict(userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<GroupIdsByUserIdCache>().getGroupIds(userId)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a batch of user-group associations changes.
     *
     * @param userIds user id collection
     */
    open fun syncOnBatchGroupUserChange(userIds: Collection<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After batch user-group association change, syncing ${CACHE_NAME} cache...")
            userIds.forEach { userId ->
                KeyValueCacheKit.evict(CACHE_NAME, userId)
                if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<GroupIdsByUserIdCache>().getGroupIds(userId)
                }
            }
            log.debug("${CACHE_NAME} cache sync complete; ${userIds.size} users affected.")
        }
    }

    /** Clear the group id list for the userId after a user delete. */
    private fun evictByUserId(userId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncOnBatchGroupUserChange(event.userIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictByUserId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.ids.forEach(::evictByUserId)
    }

    private val log = LogFactory.getLog(this::class)

}
