package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.platform.cache.AffectedUserResolver
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.source.ExternalRoleSourceChanged
import io.kudos.ms.auth.core.role.source.RoleSourceRegistry
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
 * @author AI: Claude
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
    private lateinit var authRoleDao: AuthRoleDao

    @Autowired
    private lateinit var authGroupDao: AuthGroupDao

    @Autowired
    private lateinit var affectedUserResolver: AffectedUserResolver

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    @Autowired
    private lateinit var roleSourceRegistry: RoleSourceRegistry

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
        // Deactivated roles/groups grant nothing; snapshot both id sets once instead of per user.
        val activeRoleIds = authRoleDao.searchActiveRoleIdSet()
        val activeGroupIds = authGroupDao.searchActiveGroupIdSet()

        log.debug(
            "Loaded from DB: ${users.size} users, direct-role groups ${userIdToDirectRoleIds.size}, " +
                "user-group groups ${userIdToGroupIds.size}, group-role groups ${groupIdToRoleIds.size}, " +
                "active roles ${activeRoleIds.size}, active groups ${activeGroupIds.size}."
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
                activeRoleIds = activeRoleIds,
                activeGroupIds = activeGroupIds,
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
     * The empty result is cached too. A principal holding no role is a stable fact, invalidated by
     * the same events as any other, and this method is consulted per request whenever a deployment
     * configures platform-administrator roles — leaving it uncached sent that population to the
     * database on every call. See [io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache].
     *
     * @param userId User ID
     * @return List<roleId>
     */
    @Cacheable(cacheNames = [CACHE_NAME], key = "#userId")
    open fun getRoleIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Cache miss for user ${userId}'s role IDs; loading from DB...")
        }
        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        // A deactivated group relays no roles.
        val groupIds = authGroupDao.filterActiveGroupIds(authGroupUserDao.searchGroupIdsByUserId(userId))
        val groupDerived = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        // Roles the principal holds by virtue of something outside auth — a position, an org
        // attribute. Empty unless the deployment registered a source; see IRoleSourceProvider for
        // why these are resolved rather than mirrored into auth_role_user.
        val external = roleSourceRegistry.contributedRoleIds(userId)
        // A deactivated role is not part of anyone's effective set (see AuthRoleDao.filterActiveRoleIds).
        // Contributed roles go through the same filter: a source may add a role, never exempt one.
        val effective = authRoleDao.filterActiveRoleIds((direct + groupDerived + external).distinct()).toList()
        log.debug(
            "Loaded effective roles for user ${userId} from DB: direct=${direct.size}, " +
                "group-inherited=${groupDerived.size}, external=${external.size}, active=${effective.size}."
        )
        return effective
    }

    /**
     * Merges "directly bound roles" and "roles inherited via groups" into a deduplicated effective role list,
     * dropping anything deactivated.
     *
     * Early-return path: when both sides are empty, return an empty list immediately to avoid unnecessary
     * flatMap/distinct overhead.
     *
     * @param directRoleIds Role IDs directly held by the user
     * @param groupIds Group IDs the user belongs to
     * @param groupIdToRoleIds Group ID -> role ID list held by that group (preloaded in bulk to avoid N+1 inside flatMap)
     * @param activeRoleIds Snapshot of every active role id; roles outside it contribute nothing
     * @param activeGroupIds Snapshot of every active group id; groups outside it relay nothing
     * @return Deduplicated, active-only effective role IDs
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    private fun computeEffectiveRoleIds(
        directRoleIds: Collection<String>,
        groupIds: Collection<String>,
        groupIdToRoleIds: Map<String, List<String>>,
        activeRoleIds: Set<String>,
        activeGroupIds: Set<String>,
    ): List<String> {
        if (directRoleIds.isEmpty() && groupIds.isEmpty()) return emptyList()
        val groupDerived = groupIds.filter { it in activeGroupIds }.flatMap { groupIdToRoleIds[it].orEmpty() }
        return (directRoleIds + groupDerived).distinct().filter { it in activeRoleIds }
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

    /**
     * The role row itself changed. `active` decides whether the role belongs to anyone's effective
     * set at all, so this cache must react — holders of the role *and* of its descendants (see
     * [AffectedUserResolver.usersAffectedByRoleChange]) are invalidated.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUpdated) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val userIds = affectedUserResolver.usersAffectedByRoleChange(event.id)
        if (userIds.isEmpty()) return
        syncByUserIds(userIds)
    }

    /** Deactivating a group stops it relaying roles, so its members' effective sets change. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUpdated) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val userIds = affectedUserResolver.usersAffectedByGroupChange(event.id)
        if (userIds.isEmpty()) return
        syncByUserIds(userIds)
    }

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
        val userIds = authGroupUserDao.searchMemberUserIdsByGroupId(event.groupId)
        if (userIds.isEmpty()) return
        syncByUserIds(userIds)
    }

    /**
     * An external role source changed its answer. kudos cannot observe a table it does not know
     * about, so this event is the only thing that can make the change take effect promptly — see
     * [io.kudos.ms.auth.core.role.source.IRoleSourceProvider].
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: ExternalRoleSourceChanged): Unit = syncByUserIds(event.principalIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictByUserId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.ids.forEach(::evictByUserId)
    }

    private val log = LogFactory.getLog(this::class)

}
