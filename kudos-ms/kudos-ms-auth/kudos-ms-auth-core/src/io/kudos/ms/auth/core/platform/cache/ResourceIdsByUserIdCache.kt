package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the list of resource IDs keyed by user id.
 *
 * 1. Source tables: `auth_role_user` (direct roles) UNION `auth_group_user` + `auth_group_role`
 *    (roles inherited via group), joined with `auth_role_resource` to obtain the resource ID set.
 * 2. Caches the full set of resource IDs owned by each user.
 * 3. Cache key: userId.
 * 4. Cache value: collection of resource IDs (List<String>).
 * 5. Query flow: user -> (direct roles UNION group -> roles) -> resources.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByUserIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<ResourceIdsByUserIdCache>().getResourceIds(key)

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
            val effectiveRoleIds = computeEffectiveRoleIds(
                directRoleIds = userIdToDirectRoleIds[userId].orEmpty(),
                groupIds = userIdToGroupIds[userId].orEmpty(),
                groupIdToRoleIds = groupIdToRoleIds,
            )
            if (effectiveRoleIds.isEmpty()) return@forEach
            val resourceIds = effectiveRoleIds.flatMap { roleId ->
                roleIdToResourceIdsMap[roleId] ?: emptyList()
            }.map { it.trim() }.distinct()

            if (resourceIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, userId, resourceIds)
                log.debug("Cached ${resourceIds.size} resource IDs for user ${userId}.")
            }
        }
    }

    /**
     * Get all resource IDs owned by the given user (the deduplicated union of resources reachable via
     * direct roles and group-inherited roles) from the cache. If absent, load from the database and
     * write back.
     *
     * @param userId user id
     * @return list of resource IDs
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Cache miss for resource IDs of user ${userId}; loading from the database...")
        }

        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val groupDerived = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        val effectiveRoleIds = (direct + groupDerived).distinct()
        if (effectiveRoleIds.isEmpty()) {
            log.debug("User ${userId} has no roles assigned, directly or via group inheritance.")
            return emptyList()
        }

        val resultList = authRoleResourceDao.searchResourceIdsByRoleIds(effectiveRoleIds)
        log.debug(
            "Loaded ${effectiveRoleIds.size} effective roles for user ${userId} (direct ${direct.size} + group-inherited ${groupDerived.size}) from the database, " +
                "yielding ${resultList.size} resource IDs (after deduplication)."
        )
        return resultList.toList()
    }

    /**
     * Merge directly bound roles and group-inherited roles into a deduplicated list of effective roles.
     * Short-circuits: returns an empty list immediately when both inputs are empty.
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
     * @deprecated Kept as a single-user public entry for backward compatibility; new code should rely
     * on the event mechanism (such as [AuthRoleUserRelationsChanged]).
     */
    open fun syncOnRoleUserChange(userId: String): Unit = syncByUserIds(listOf(userId))

    /**
     * @deprecated Kept as a batch public entry for backward compatibility; new code should rely on the
     * event mechanism.
     */
    open fun syncOnBatchRoleUserChange(userIds: Collection<String>): Unit = syncByUserIds(userIds)

    /** Invalidate the cache in batch after the user list changes. */
    private fun syncByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        userIds.forEach { userId ->
            KeyValueCacheKit.evict(CACHE_NAME, userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByUserIdCache>().getResourceIds(userId)
            }
        }
        log.debug("${CACHE_NAME} cache sync complete; ${userIds.size} users affected.")
    }

    /**
     * Sync the cache after a role-resource relation change (must locate every user associated with the role).
     * Note: affected users include a) users who directly hold the role and b) users who inherit it via a group.
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("Role ${roleId} resource relations changed; syncing the ${CACHE_NAME} cache...")

        // Users who directly hold the role.
        val roleUserCriteria = Criteria(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
        val directUserIds = authRoleUserDao.search(roleUserCriteria).map { it.userId }.distinct()

        // Users who inherit the role via a group: locate every group holding the role, then expand to userIds.
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroupUserIds = groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }.distinct()

        val allUserIds = (directUserIds + viaGroupUserIds).distinct()
        if (allUserIds.isEmpty()) return
        syncByUserIds(allUserIds)
        log.debug(
            "${CACHE_NAME} cache sync complete; ${allUserIds.size} users affected " +
                "(direct ${directUserIds.size} + group-inherited ${viaGroupUserIds.size})."
        )
    }

    /** Evict the resourceId list for the given userId after the user is deleted. */
    private fun evictByUserId(userId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleResourceRelationsChanged): Unit = syncOnRoleResourceChange(event.roleId)

    /** Users joining or leaving a group changes their effective role set, so recompute the resources. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /** Roles bound to a group changed, so resources for every user in the group must be recomputed. */
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
