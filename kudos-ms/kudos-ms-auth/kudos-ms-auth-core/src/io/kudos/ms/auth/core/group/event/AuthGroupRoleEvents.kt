package io.kudos.ms.auth.core.group.event


/**
 * Group-role association (`auth_group_role`) domain events. Join table affecting these projections:
 * - RoleIdsByUserIdCache: the "effective role set" for every user in the group must be recomputed
 * - ResourceIdsByUserIdCache: ditto for the resource set
 *
 * Note: this event carries groupId rather than userIds -- listeners must expand it to the affected
 * user list using [io.kudos.ms.auth.core.group.dao.AuthGroupUserDao.searchUserIdsByGroupId]. The reason:
 * the publisher [io.kudos.ms.auth.core.group.service.impl.AuthGroupRoleService] only knows about the
 * (groupId, roleIds) change dimension and should not be responsible for "which users are in this group".
 *
 * @author K
 * @since 1.0.0
 */
sealed interface AuthGroupRoleEvent

/** A batch of group-role association changes (bind or unbind). */
data class AuthGroupRoleRelationsChanged(
    val groupId: String,
    val roleIds: Collection<String>,
) : AuthGroupRoleEvent
