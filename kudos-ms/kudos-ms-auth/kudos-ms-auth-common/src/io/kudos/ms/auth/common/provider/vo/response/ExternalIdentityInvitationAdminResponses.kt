package io.kudos.ms.auth.common.provider.vo.response

import java.time.LocalDateTime

/** The token is shown only in this create response and cannot be recovered later. */
data class ExternalIdentityInvitationAdminCreatedResponse(
    val invitationId: String,
    val token: String,
    val expiresAt: LocalDateTime,
    val maxUses: Int,
)
