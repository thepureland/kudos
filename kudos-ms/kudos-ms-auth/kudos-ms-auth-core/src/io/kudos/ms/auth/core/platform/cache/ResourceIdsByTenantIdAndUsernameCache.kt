package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the per-user resource ID list keyed by (tenantId, username).
 *
 * Parallels [ResourceIdsByUserIdCache] exactly — the only difference is that the key is
 * (tenantId, username) rather than userId. Sources: `user_account` UNION `auth_role_user`
 * (direct roles) UNION `auth_group_user` + `auth_group_role` (roles inherited via group),
 * joined with `auth_role_resource` to obtain the resource ID set.
 *
 * Cache key: tenantId::username; value: List<String>.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByTenantIdAndUsernameCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    companion object Companion {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_USERNAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache ${CACHE_NAME} key format must be tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}username"
        }
        val tenantAndUsername = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdsByTenantIdAndUsernameCache>().getResourceIds(
            tenantAndUsername[0], tenantAndUsername[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skipping load and cache of resource IDs for all users.")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToDirectRoleIds = authRoleUserDao.searchAllUserIdToRoleIdsForCache()
        val userIdToGroupIds = authGroupUserDao.searchAllUserIdToGroupIdsForCache()
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val roleIdToResourceIdsMap = authRoleResourceDao.searchAllRoleIdToResourceIdsForCache()

        log.debug(
            "Loaded ${users.size} users, ${userIdToDirectRoleIds.size} direct-role groups, " +
                "${userIdToGroupIds.size} user-group groups, ${groupIdToRoleIds.size} group-role groups, " +
                "and ${roleIdToResourceIdsMap.size} role-resource groups from the database."
        )

        if (clear) {
            clear()
        }

        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val tenantId = user.tenantId ?: return@forEach
            val username = user.username ?: return@forEach
            val effectiveRoleIds = computeEffectiveRoleIds(
                directRoleIds = userIdToDirectRoleIds[userId].orEmpty(),
                groupIds = userIdToGroupIds[userId].orEmpty(),
                groupIdToRoleIds = groupIdToRoleIds,
            )
            if (effectiveRoleIds.isEmpty()) return@forEach
            val resourceIds = effectiveRoleIds.flatMap { roleId ->
                roleIdToResourceIdsMap[roleId] ?: emptyList()
            }.distinct()

            if (resourceIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, username), resourceIds)
                log.debug("Cached ${resourceIds.size} resource IDs for tenant ${tenantId} user ${username}.")
            }
        }
    }

    /**
     * Get all resource IDs owned by the given user (the deduplicated union of resources reachable via
     * direct roles and group-inherited roles) from the cache, keyed by (tenantId, username).
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#username)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, username: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Cache miss for resource IDs of tenant ${tenantId} user ${username}; loading from the database...")
        }

        val userId = userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
        if (userId == null) {
            log.debug("User ${username} not found in tenant ${tenantId}.")
            return emptyList()
        }

        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val viaGroup = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        val effectiveRoleIds = (direct + viaGroup).distinct()
        if (effectiveRoleIds.isEmpty()) {
            log.debug("User ${username} has no roles assigned, directly or via group inheritance.")
            return emptyList()
        }

        val resultList = authRoleResourceDao.searchResourceIdsByRoleIds(effectiveRoleIds)
        log.debug(
            "Loaded ${effectiveRoleIds.size} effective roles for tenant ${tenantId} user ${username} " +
                "(direct ${direct.size} + group-inherited ${viaGroup.size}) from the database, yielding ${resultList.size} resource IDs."
        )
        return resultList.toList()
    }

    /**
     * Merge directly bound roles and group-inherited roles into a deduplicated list of effective roles.
     * Short-circuits: returns an empty list immediately when both inputs are empty.
     *
     * Note: an identical helper exists in RoleIdsByUserIdCache / ResourceIdsByUserIdCache. It has not
     * been extracted to a top-level util historically to avoid introducing cross-module dependencies;
     * if the caller count grows further, consider pushing it down to the base layer.
     *
     * @param directRoleIds role id collection directly held by the user
     * @param groupIds group id collection the user belongs to
     * @param groupIdToRoleIds group id -> list of role ids held by that group (pre-loaded in batch to avoid N+1)
     * @return deduplicated list of effective role ids
     * @author K
     * @since 1.0.0
     */
    private fun computeEffectiveRoleIds(
        directRoleIds: Collection<String>,
        groupIds: Collection<String>,
        groupIdToRoleIds: Map<String, List<String>>,
    ): List<String> {
        if (directRoleIds.isEmpty() && groupIds.isEmpty()) return emptyList()
        val groupDerived = groupIds.flatMap { groupIdToRoleIds[it].orEmpty() }
        return (directRoleIds + groupDerived).distinct()
    }

    /**
     * Sync the cache after user information is updated (username or tenant changed).
     */
    open fun syncOnUserUpdate(oldTenantId: String, oldUsername: String, newTenantId: String, newUsername: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("User information updated; syncing the ${CACHE_NAME} cache...")

        KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldUsername))
        if (oldTenantId != newTenantId || oldUsername != newUsername) {
            KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newUsername))
        }
        log.debug("${CACHE_NAME} cache sync complete.")
    }

    /** @deprecated Kept under the old name for backward compatibility; new code should rely on the event mechanism (such as [AuthGroupUserRelationsChanged]). */
    open fun syncOnRoleUserChange(tenantId: String, username: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("User ${username} role relations changed; syncing the ${CACHE_NAME} cache...")
        evict(getKey(tenantId, username))
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<ResourceIdsByTenantIdAndUsernameCache>().getResourceIds(tenantId, username)
        }
    }

    /**
     * Sync the cache after a role-resource relation change (must locate every user associated with the role).
     * Note: affected users include a) users who directly hold the role and b) users who inherit it via a group.
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("Role ${roleId} resource relations changed; syncing the ${CACHE_NAME} cache...")

        val directUserIds = authRoleUserDao.searchUserIdsByRoleId(roleId)
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroupUserIds = groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }.distinct()
        val allUserIds = (directUserIds + viaGroupUserIds).distinct()
        if (allUserIds.isEmpty()) return

        val users = userAccountDao.getByIdsAs<UserAccountCacheEntry>(allUserIds)
        users.forEach { user ->
            val t = user.tenantId ?: return@forEach
            val u = user.username ?: return@forEach
            KeyValueCacheKit.evict(CACHE_NAME, getKey(t, u))
            log.debug("Evicted the resource cache for user ${u}.")
        }
        log.debug(
            "${CACHE_NAME} cache sync complete; ${allUserIds.size} users affected " +
                "(direct ${directUserIds.size} + group-inherited ${viaGroupUserIds.size})."
        )
    }

    fun getKey(tenantId: String, username: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${username}"
    }

    /** Evict the local entry for (tenantId, username) only. */
    private fun evictBy(tenantId: String, username: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, username))
    }

    /** Resolve (tenantId, username) in batch from userIds and evict the corresponding cache entries. */
    private fun evictByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (userIds.isEmpty()) return
        val users = userAccountDao.getByIdsAs<UserAccountCacheEntry>(userIds)
        users.forEach { user ->
            val t = user.tenantId ?: return@forEach
            val u = user.username ?: return@forEach
            KeyValueCacheKit.evict(CACHE_NAME, getKey(t, u))
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictBy(event.tenantId, event.username)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.username) }
    }

    /** Users joining or leaving a group changes their resource set. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = evictByUserIds(event.userIds)

    /** Roles bound to a group changed, so resources for every user in the group must be recomputed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val userIds = authGroupUserDao.searchUserIdsByGroupId(event.groupId)
        if (userIds.isEmpty()) return
        evictByUserIds(userIds)
    }

    private val log = LogFactory.getLog(this::class)

}
