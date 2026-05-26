package io.kudos.ms.user.core.org.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Org ID list (by user id) cache handler
 *
 * 1. Source table: user_org_user
 * 2. Caches the list of all org IDs each user belongs to
 * 3. Cache key: userId
 * 4. Cache value: list of org IDs (List<String>)
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class OrgIdsByUserIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "USER_ORG_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<OrgIdsByUserIdCache>().getOrgIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skip loading and caching all users' org IDs!")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToOrgIdsMap = userOrgUserDao.searchAllUserIdToOrgIds()

        log.debug("Loaded ${users.size} users from database, ${userIdToOrgIdsMap.size} org-user relation groups.")

        // Clear the cache
        if (clear) {
            clear()
        }

        // Cache the org ID list per user
        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val orgIds = userIdToOrgIdsMap[userId] ?: emptyList()
            if (orgIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, userId, orgIds)
                log.debug("Cached ${orgIds.size} org IDs for user ${userId}.")
            }
        }
    }

    /**
     * Get the org IDs of the given user from the cache; if missing, load from the database and write back.
     *
     * @param userId user ID
     * @return list of org IDs
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getOrgIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("No org IDs for user ${userId} in the cache; loading from the database...")
        }

        val orgIds = userOrgUserDao.searchOrgIdsByUserId(userId)
        log.debug("Loaded ${orgIds.size} org IDs for user ${userId} from the database.")
        return orgIds
    }

    /**
     * Sync the cache after a user-org relation change
     *
     * @param userId user ID
     */
    open fun syncOnOrgUserChange(userId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Org relations for user ${userId} changed; syncing ${CACHE_NAME} cache...")
            evict(userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<OrgIdsByUserIdCache>().getOrgIds(userId)
            }
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync the cache after batch user-org relation changes
     *
     * @param userIds user ID collection
     */
    open fun syncOnBatchOrgUserChange(userIds: Collection<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Batch user-org relations changed; syncing ${CACHE_NAME} cache...")
            userIds.forEach { userId ->
                KeyValueCacheKit.evict(CACHE_NAME, userId)
                if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<OrgIdsByUserIdCache>().getOrgIds(userId)
                }
            }
            log.debug("${CACHE_NAME} cache sync completed; ${userIds.size} users affected.")
        }
    }

    /** Clear the orgId list for the given userId after a user is deleted. */
    private fun evictByUserId(userId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUserRelationsChanged) {
        syncOnBatchOrgUserChange(event.userIds)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictByUserId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.ids.forEach(::evictByUserId)
    }

    private val log = LogFactory.getLog(this::class)

}