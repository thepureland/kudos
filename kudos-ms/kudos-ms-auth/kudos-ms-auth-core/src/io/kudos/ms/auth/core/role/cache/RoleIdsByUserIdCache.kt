package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for role-ID lists keyed by user id.
 *
 * **Semantically the "effective role set"**:
 * - Data sources: `auth_role_user` (direct grants) ∪ `auth_group_user` + `auth_group_role` (inherited via groups)
 * - The cached value is the deduplicated union of role IDs from both paths
 *
 * Group inheritance is included because the downstream consumers of this cache
 * ([io.kudos.ms.auth.core.role.service.impl.AuthRoleService.hasRole] / [getUserRoles] / [getUserRoleIds] /
 * resource caches) ask "which roles does this user effectively have for permission checks", not
 * "which roles are directly bound". Callers that need the latter should call
 * [io.kudos.ms.auth.core.role.dao.AuthRoleUserDao.searchRoleIdsByUserId] directly.
 *
 * Cache key: userId; value: List<String>.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class RoleIdsByUserIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "AUTH_ROLE_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<RoleIdsByUserIdCache>().getRoleIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skipping load and cache of all users' role IDs!")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToDirectRoleIds = authRoleUserDao.searchAllUserIdToRoleIdsForCache()
        val userIdToGroupIds = authGroupUserDao.searchAllUserIdToGroupIdsForCache()
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()

        log.debug(
            "Loaded from DB: ${users.size} users, direct-role groups ${userIdToDirectRoleIds.size}, " +
                "user-group groups ${userIdToGroupIds.size}, group-role groups ${groupIdToRoleIds.size}."
        )

        if (clear) {
            clear()
        }

        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val effectiveRoleIds = computeEffectiveRoleIds(
                directRoleIds = userIdToDirectRoleIds[userId].orEmpty(),
                groupIds = userIdToGroupIds[userId].orEmpty(),
                groupIdToRoleIds = groupIdToRoleIds,
            )
            if (effectiveRoleIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, userId, effectiveRoleIds)
                log.debug("Cached ${effectiveRoleIds.size} effective role IDs for user ${userId}.")
            }
        }
    }

    /**
     * Returns the effective role-ID list for a user from the cache, loading from the DB and writing back on a miss.
     * Effective = direct grants ∪ inherited via user groups.
     *
     * @param userId User ID
     * @return List<roleId>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getRoleIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Cache miss for user ${userId}'s role IDs; loading from DB...")
        }
        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val groupDerived = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        val effective = (direct + groupDerived).distinct()
        log.debug(
            "Loaded effective roles for user ${userId} from DB: direct=${direct.size}, group-inherited=${groupDerived.size}, distinct=${effective.size}."
        )
        return effective
    }

    /**
     * Merges "directly bound roles" and "roles inherited via groups" into a deduplicated effective role list.
     *
     * Early-return path: when both sides are empty, return an empty list immediately to avoid unnecessary
     * flatMap/distinct overhead.
     *
     * @param directRoleIds Role IDs directly held by the user
     * @param groupIds Group IDs the user belongs to
     * @param groupIdToRoleIds Group ID -> role ID list held by that group (preloaded in bulk to avoid N+1 inside flatMap)
     * @return Deduplicated effective role IDs
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
     * @deprecated Public single-user entry kept for backward compatibility; new code should use the event mechanism
     *   ([AuthRoleUserRelationsChanged], etc.).
     */
    open fun syncOnRoleUserChange(userId: String): Unit = syncByUserIds(listOf(userId))

    /**
     * @deprecated Public batch entry kept for backward compatibility; new code should use the event mechanism.
     */
    open fun syncOnBatchRoleUserChange(userIds: Collection<String>): Unit = syncByUserIds(userIds)

    /**
     * Syncs the cache after a user-role / user-group / group-role relation change: simply invalidate by userIds.
     */
    private fun syncByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        userIds.forEach { userId ->
            KeyValueCacheKit.evict(CACHE_NAME, userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RoleIdsByUserIdCache>().getRoleIds(userId)
            }
        }
        log.debug("${CACHE_NAME} cache sync complete; ${userIds.size} users affected.")
    }

    /** Clears the role-ID list for the given userId after the user is deleted. */
    private fun evictByUserId(userId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /** A user added to or removed from a group is equivalent to a direct role-set change. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /**
     * When the roles bound to a group change, every user in that group has their effective role set recomputed.
     * Expand the groupId into userIds and reuse the unified invalidation path.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val userIds = authGroupUserDao.searchUserIdsByGroupId(event.groupId)
        if (userIds.isEmpty()) return
        syncByUserIds(userIds)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictByUserId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.ids.forEach(::evictByUserId)
    }

    private val log = LogFactory.getLog(this::class)

}
