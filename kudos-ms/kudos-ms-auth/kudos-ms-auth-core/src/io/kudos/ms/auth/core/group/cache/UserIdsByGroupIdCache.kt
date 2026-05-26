package io.kudos.ms.auth.core.group.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the list of user ids keyed by group id.
 *
 * 1. Source table: auth_group_user
 * 2. Caches the list of user ids that belong to each group
 * 3. Cache key: groupId
 * 4. Cache value: list of user ids (List<String>)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserIdsByGroupIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authGroupDao: AuthGroupDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_GROUP_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByGroupIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; not loading or caching user ids for all groups!")
            return
        }

        val groups = authGroupDao.searchActiveGroupsForCache()
        val groupIdToUserIdsMap = authGroupUserDao.searchAllGroupIdToUserIdsForCache()

        log.debug("Loaded ${groups.size} groups and ${groupIdToUserIdsMap.size} group-user groupings from DB.")

        // Clear the cache.
        if (clear) {
            clear()
        }

        // Cache the user id list per group.
        groups.forEach { group ->
            val groupId = group.id
            if (groupId.isBlank()) return@forEach
            val userIds = groupIdToUserIdsMap[groupId] ?: emptyList()
            if (userIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, groupId, userIds)
                log.debug("Cached ${userIds.size} user ids for group ${groupId}.")
            }
        }
    }

    /**
     * Get all user ids that belong to a group keyed by group id; on cache miss, load from DB and write back.
     *
     * @param groupId group id
     * @return list of user ids
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#groupId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(groupId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("User ids for group ${groupId} not in cache; loading from DB...")
        }

        val userIds = authGroupUserDao.searchUserIdsByGroupId(groupId)
        log.debug("Loaded ${userIds.size} user ids from DB for group ${groupId}.")
        return userIds.toList()
    }

    /**
     * Sync the cache after a group-user association changes.
     *
     * @param groupId group id
     */
    open fun syncOnGroupUserChange(groupId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After group ${groupId} user associations changed, syncing ${CACHE_NAME} cache...")
            evict(groupId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByGroupIdCache>().getUserIds(groupId)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a batch of group-user associations changes.
     *
     * @param groupIds group id collection
     */
    open fun syncOnBatchGroupUserChange(groupIds: Collection<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After batch group-user association change, syncing ${CACHE_NAME} cache...")
            groupIds.forEach { groupId ->
                KeyValueCacheKit.evict(CACHE_NAME, groupId)
                if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<UserIdsByGroupIdCache>().getUserIds(groupId)
                }
            }
            log.debug("${CACHE_NAME} cache sync complete; ${groupIds.size} groups affected.")
        }
    }

    /** Clear the user id list for the groupId after a group delete. */
    private fun evictByGroupId(groupId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, groupId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncOnGroupUserChange(event.groupId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupDeleted): Unit = evictByGroupId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupBatchDeleted) {
        event.ids.forEach(::evictByGroupId)
    }

    private val log = LogFactory.getLog(this::class)

}
