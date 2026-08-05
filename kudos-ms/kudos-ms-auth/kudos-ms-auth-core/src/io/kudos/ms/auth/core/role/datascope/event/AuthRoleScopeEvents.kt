package io.kudos.ms.auth.core.role.datascope.event

/**
 * `auth_role_scope` domain events.
 *
 * Deliberately distinct from [io.kudos.ms.auth.core.role.event.AuthRoleUpdated], even though both
 * change what a role means: a scope-grant edit affects only the resolved *data* scope, whereas
 * `AuthRoleUpdated` also invalidates the role-id and permission-grant caches. Reusing it here would
 * flush three caches to fix one.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
sealed interface AuthRoleScopeEvent

/** The scope values granted to a role changed on at least one dimension. */
data class AuthRoleScopeChanged(val roleId: String) : AuthRoleScopeEvent
