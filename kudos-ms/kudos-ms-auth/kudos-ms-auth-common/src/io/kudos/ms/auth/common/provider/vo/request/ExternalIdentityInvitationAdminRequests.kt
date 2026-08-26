package io.kudos.ms.auth.common.provider.vo.request

import java.time.LocalDateTime

/** Issues a one-time invitation for an existing local user and one provider instance. */
data class ExternalIdentityInvitationAdminCreateRequest(
    val userId: String,
    val providerId: String,
    val expectedEmail: String? = null,
    val expiresAt: LocalDateTime,
    val reason: String,
)

data class ExternalIdentityInvitationAdminRevokeRequest(
    val invitationId: String,
    val reason: String,
)
