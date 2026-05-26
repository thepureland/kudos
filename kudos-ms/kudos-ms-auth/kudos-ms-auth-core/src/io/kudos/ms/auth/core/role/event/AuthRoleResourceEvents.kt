package io.kudos.ms.auth.core.role.event

/**
 * Role-resource association (`auth_role_resource`) domain events. Join table affecting two cache projections:
 * - ResourceIdsByRoleIdCache: lookup resourceIds by roleId
 * - ResourceIdsByUserIdCache: lookup resourceIds by userId (aggregated through roles; must be invalidated)
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthRoleResourceEvent

/** A batch of role-resource association changes (bind or unbind); listeners invalidate caches by roleId. */
data class AuthRoleResourceRelationsChanged(
    val roleId: String,
    val resourceIds: Collection<String>,
) : AuthRoleResourceEvent
