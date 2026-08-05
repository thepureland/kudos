package io.kudos.ms.auth.core.version.event

/**
 * Token-epoch domain events.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
sealed interface AuthPrincipalVersionEvent

/**
 * An administrator forced every token this principal holds to become stale.
 *
 * Distinct from the permission-change events on purpose: this fires precisely when the permissions
 * did *not* change, so nothing else in the module would have announced it.
 */
data class PrincipalTokenEpochBumped(
    val principalId: String,
    val epoch: Long,
    val reason: String?,
) : AuthPrincipalVersionEvent
