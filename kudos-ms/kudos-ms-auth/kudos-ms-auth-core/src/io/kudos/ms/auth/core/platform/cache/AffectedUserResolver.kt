package io.kudos.ms.auth.core.platform.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * Resolves "which users does this change affect" for the permission caches.
 *
 * Both [io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache] and [ResourceIdsByUserIdCache] have to
 * answer the same question when a role row itself mutates, and the answer is subtle enough that
 * duplicating it would guarantee the two caches drift apart. It lives here once.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class AffectedUserResolver {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    /**
     * Every user whose effective permissions can change when the role row [roleId] itself mutates
     * (`active`, `parent_id`, `data_scope`, …).
     *
     * The set is **not** just the role's holders. Because a child role inherits its parents'
     * resources (see [ResourceIdsByUserIdCache]), holders of any *descendant* of [roleId] consume
     * this role's resources too, and are equally invalidated. Both populations are then expanded
     * along the two grant paths — direct grants and group membership.
     *
     * @param roleId the role whose own row changed
     * @return the affected user ids, deduplicated; empty when nobody holds the affected roles
     */
    open fun usersAffectedByRoleChange(roleId: String): Set<String> {
        val roleIds = authRoleDao.searchDescendantRoleIds(roleId) + roleId
        return usersHoldingAnyOf(roleIds)
    }

    /**
     * Every user holding any role in [roleIds], via a direct grant or via group membership.
     *
     * @param roleIds the roles to expand
     * @return the holder user ids, deduplicated
     */
    open fun usersHoldingAnyOf(roleIds: Collection<String>): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val direct = roleIds.flatMap { authRoleUserDao.searchUserIdsByRoleId(it) }
        val viaGroup = roleIds
            .flatMap { authGroupRoleDao.searchGroupIdsByRoleId(it) }
            .distinct()
            .flatMap { authGroupUserDao.searchMemberUserIdsByGroupId(it) }
        return (direct + viaGroup).toSet()
    }

    /**
     * Every user in the group [groupId]. Groups are flat, so this is a direct lookup — the method
     * exists so callers do not have to reach for the group DAO just for cache invalidation.
     *
     * Deliberately the raw roster, not the in-force one: this answers "whose cache is now wrong",
     * and a future-dated or just-lapsed member is precisely the case whose cached snapshot needs
     * rebuilding. Evicting more than strictly necessary costs a reload; evicting less leaves a
     * stale permission set with nothing left to correct it.
     *
     * @param groupId the group whose own row changed
     * @return the member user ids
     */
    open fun usersAffectedByGroupChange(groupId: String): Set<String> =
        authGroupUserDao.searchMemberUserIdsByGroupId(groupId)

}
