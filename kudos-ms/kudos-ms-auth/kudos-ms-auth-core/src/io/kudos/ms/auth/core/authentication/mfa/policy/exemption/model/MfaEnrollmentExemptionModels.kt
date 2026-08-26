package io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model

import java.time.LocalDateTime

enum class MfaEnrollmentExemptionStatusEnum {
    ACTIVE,
    REVOKED,
}

/**
 * One administrator-granted, time-boxed pass on the *enrollment* requirement.
 *
 * It never exempts a user who has enrolled from presenting their second factor: that would be an
 * administrator-triggered way around a credential the user still holds. The only block it lifts is
 * "you must enrol before you can sign in", which is the case an operator cannot otherwise resolve
 * without widening the whole tenant's policy.
 */
data class MfaEnrollmentExemption(
    val id: String,
    val tenantId: String,
    val userId: String,
    val status: MfaEnrollmentExemptionStatusEnum,
    val reason: String,
    val grantedBy: String,
    val grantedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val revokedBy: String?,
    val revokeReason: String?,
    val revokedAt: LocalDateTime?,
)

data class MfaEnrollmentExemptionGrantCommand(
    val tenantId: String,
    val userId: String,
    val expiresAt: LocalDateTime,
    val actorUserId: String,
    val reason: String,
)

data class MfaEnrollmentExemptionRevokeCommand(
    val tenantId: String,
    val userId: String,
    val actorUserId: String,
    val reason: String,
)
