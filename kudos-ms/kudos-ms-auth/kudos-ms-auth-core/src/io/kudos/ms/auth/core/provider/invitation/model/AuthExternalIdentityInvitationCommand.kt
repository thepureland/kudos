package io.kudos.ms.auth.core.provider.invitation.model

import java.time.LocalDateTime

/** Trusted administrator command for issuing a one-time external-identity invitation. */
data class AuthExternalIdentityInvitationCreateCommand(
    val tenantId: String,
    val userId: String,
    val identityProviderId: String,
    val expectedEmail: String? = null,
    val expiresAt: LocalDateTime,
    val actorUserId: String,
    val operationReason: String,
)

/** The raw bearer token is returned once and is never persisted. */
data class AuthExternalIdentityInvitationCreated(
    val invitationId: String,
    val token: String,
    val expiresAt: LocalDateTime,
    val maxUses: Int,
)

/** Secret-free reference attached to a server-side authentication transaction. */
data class AuthExternalIdentityInvitationReference(
    val invitationId: String,
)
