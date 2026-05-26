package io.kudos.ms.auth.core.group.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the list of user ids keyed by (tenantId, groupCode).
 *
 * 1. Source tables: auth_group + auth_group_user
 * 2. Caches the user id set of the specified group per tenant
 * 3. Cache key: tenantId::groupCode
 * 4. Cache value: list of user ids (List<String>)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserIdsByTenantIdAndGroupCodeCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authGroupHashCache: AuthGroupHashCache

    @Autowired
    private lateinit var authGroupDao: AuthGroupDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_TENANT_ID_AND_GROUP_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache ${CACHE_NAME} key format must be tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}groupCode"
        }
        val tenantAndGroupCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<UserIdsByTenantIdAndGroupCodeCache>().getUserIds(
            tenantAndGroupCode[0], tenantAndGroupCode[1]
        )
    }

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

        // Cache the user id lists.
        groups.forEach { group ->
            val groupId = group.id
            if (groupId.isBlank()) return@forEach
            val tenantId = group.tenantId ?: return@forEach
            val groupCode = group.code ?: return@forEach
            val userIds = groupIdToUserIdsMap[groupId] ?: emptyList()
            KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, groupCode), userIds)
            log.debug("Cached ${userIds.size} user ids for tenant=${group.tenantId} group=${group.code}.")
        }
    }

    /**
     * Get all user ids under a group keyed by (tenantId, groupCode); on cache miss, load from DB and write back.
     *
     * @param tenantId tenant id
     * @param groupCode group code
     * @return list of user ids
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#groupCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(tenantId: String, groupCode: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("User ids for tenant=${tenantId} group=${groupCode} not in cache; loading from DB...")
        }

        // 1. Look up the group id from the cache (avoid hitting the DB).
        val groupId = authGroupHashCache.getGroupByTenantIdAndGroupCode(tenantId, groupCode)?.id

        if (groupId == null) {
            log.debug("Group not found for tenant=${tenantId} code=${groupCode}.")
            return emptyList()
        }

        val userIds = authGroupUserDao.searchUserIdsByGroupId(groupId)
        log.debug("Loaded ${userIds.size} user ids from DB for tenant=${tenantId} group=${groupCode}.")
        return userIds.toList()
    }

    /**
     * Sync the cache after a group-user association is inserted.
     *
     * @param tenantId tenant id
     * @param groupCode group code
     */
    open fun syncOnGroupUserInsert(tenantId: String, groupCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After inserting group-user association tenant=${tenantId} group=${groupCode}, syncing ${CACHE_NAME} cache...")
            evict(getKey(tenantId, groupCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByTenantIdAndGroupCodeCache>().getUserIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a group-user association is deleted.
     *
     * @param tenantId tenant id
     * @param groupCode group code
     */
    open fun syncOnGroupUserDelete(tenantId: String, groupCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After deleting group-user association tenant=${tenantId} group=${groupCode}, syncing ${CACHE_NAME} cache...")
            evict(getKey(tenantId, groupCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByTenantIdAndGroupCodeCache>().getUserIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after group info is updated (group code or active flag changed).
     *
     * @param oldTenantId previous tenant id
     * @param oldGroupCode previous group code
     * @param newTenantId new tenant id (equal to the old one if unchanged)
     * @param newGroupCode new group code (equal to the old one if unchanged)
     */
    open fun syncOnGroupUpdate(oldTenantId: String, oldGroupCode: String, newTenantId: String, newGroupCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After group info update, syncing ${CACHE_NAME} cache...")

            // Evict the old cache entry.
            KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldGroupCode))

            // If code or tenant changed, also evict the new cache entry (if present).
            if (oldTenantId != newTenantId || oldGroupCode != newGroupCode) {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newGroupCode))
            }

            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Build the cache key by joining the given parameters.
     *
     * @param tenantId tenant id
     * @param groupCode group code
     * @return cache key
     */
    fun getKey(tenantId: String, groupCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${groupCode}"
    }

    /** Local (tenantId, groupCode) eviction only; AuthGroupHashCache subscribes to AuthGroupDeleted separately. */
    private fun evictBy(tenantId: String, groupCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, groupCode))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupDeleted): Unit = evictBy(event.tenantId, event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.code) }
    }

    private val log = LogFactory.getLog(this::class)

}
