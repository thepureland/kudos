package io.kudos.ms.auth.core.role.event

/**
 * Role-User relation (`auth_role_user`) domain events. Join table, affecting three cache projections:
 * - UserIdsByRoleIdCache: query userIds by roleId
 * - RoleIdsByUserIdCache: query roleIds by userId
 * - ResourceIdsByUserIdCache: query resourceIds by userId (aggregated through roles, requires invalidation)
 *
 * Relation changes affect both the roleId side and the involved userIds side.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthRoleUserEvent

/** A batch of role-user relation changes (bind or unbind); listener invalidates caches three-way by roleId / userIds. */
data class AuthRoleUserRelationsChanged(
    val roleId: String,
    val userIds: Collection<String>,
) : AuthRoleUserEvent
