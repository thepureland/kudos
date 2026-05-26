package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for user-id lists keyed by role id.
 *
 * **Semantics: "effective user set"**:
 * - Source tables: `auth_role_user` (direct binding) UNION `auth_group_role` + `auth_group_user` (group inheritance).
 * - The cached value is the deduplicated union of user IDs reached through either path.
 *
 * Group-inherited users are included because the question "which users does this role have" is, for
 * permission evaluation purposes, equivalent to "which users does this role actually affect" — users
 * who reach the role via a group should not be excluded.
 * Callers that need only "users directly bound to the role" should call
 * [AuthRoleUserDao.searchUserIdsByRoleId] instead.
 *
 * Cache key: roleId; value: List<String>.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByRoleIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_ROLE_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByRoleIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache not enabled; skip loading and caching user IDs for all roles!")
            return
        }

        val roles = authRoleDao.searchActiveRolesForCache()
        val roleIdToDirectUserIds = authRoleUserDao.getAllRoleIdToUserIdsForCache()
        // Invert group->roles to role->groups so "which groups hold this role" is a map lookup
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val groupIdToUserIds = authGroupUserDao.searchAllGroupIdToUserIdsForCache()
        val roleIdToGroupIds: Map<String, List<String>> = buildMap<String, MutableList<String>> {
            groupIdToRoleIds.forEach { (groupId, roleIds) ->
                roleIds.forEach { roleId ->
                    getOrPut(roleId) { mutableListOf() }.add(groupId)
                }
            }
        }

        log.debug(
            "Loaded ${roles.size} roles from DB; direct role-user groupings: ${roleIdToDirectUserIds.size}; " +
                "group-role groupings: ${groupIdToRoleIds.size}; group-user groupings: ${groupIdToUserIds.size}."
        )

        if (clear) {
            clear()
        }

        roles.forEach { role ->
            val roleId = role.id
            if (roleId.isBlank()) return@forEach
            val direct = roleIdToDirectUserIds[roleId].orEmpty()
            val viaGroup = roleIdToGroupIds[roleId].orEmpty().flatMap { gid ->
                groupIdToUserIds[gid].orEmpty()
            }
            val all = (direct + viaGroup).distinct()
            if (all.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, roleId, all)
                log.debug("Cached ${all.size} effective user IDs for role ${roleId} (direct ${direct.size} + group-inherited ${viaGroup.size}).")
            }
        }
    }

    /**
     * Returns all user IDs holding the given role from the cache (direct binding UNION group inheritance);
     * loads from the database and writes back on a cache miss.
     *
     * @param roleId role id
     * @return List<user id>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#roleId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(roleId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("No cached user IDs for role ${roleId}; loading from DB...")
        }
        val direct = authRoleUserDao.searchUserIdsByRoleId(roleId)
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroup = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }
        }
        val effective = (direct + viaGroup).distinct()
        log.debug(
            "Loaded effective users for role ${roleId} from DB: direct ${direct.size}, group-inherited ${viaGroup.size}, deduplicated ${effective.size}."
        )
        return effective
    }

    /**
     * @deprecated Kept as a public single-role entry point for backward compatibility; new code should use the event mechanism.
     */
    open fun syncOnRoleUserChange(roleId: String): Unit = syncByRoleIds(listOf(roleId))

    /**
     * @deprecated Kept as a public batch entry point for backward compatibility; new code should use the event mechanism.
     */
    open fun syncOnBatchRoleUserChange(roleIds: Collection<String>): Unit = syncByRoleIds(roleIds)

    private fun syncByRoleIds(roleIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        roleIds.forEach { roleId ->
            KeyValueCacheKit.evict(CACHE_NAME, roleId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByRoleIdCache>().getUserIds(roleId)
            }
        }
        log.debug("${CACHE_NAME} cache sync complete; ${roleIds.size} roles affected.")
    }

    /** After a role is deleted, evict the user-id list under that roleId. */
    private fun evictByRoleId(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, roleId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByRoleIds(listOf(event.roleId))

    /**
     * A user joining/leaving a group => the "effective user set" of every role bound to that group must be recomputed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val roleIds = authGroupRoleDao.searchRoleIdsByGroupId(event.groupId)
        if (roleIds.isEmpty()) return
        syncByRoleIds(roleIds)
    }

    /**
     * The roles bound to a group changed => the "effective user set" of those roles must be recomputed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged): Unit = syncByRoleIds(event.roleIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = evictByRoleId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted) {
        event.ids.forEach(::evictByRoleId)
    }

    private val log = LogFactory.getLog(this::class)

}
