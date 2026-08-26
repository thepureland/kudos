package io.kudos.ms.auth.common.authentication.mfa.policy.vo

import java.time.LocalDateTime

/**
 * Grants one user a bounded pass on the MFA enrollment requirement.
 *
 * No tenant or operator field: both come from the trusted administrator context, and an exemption is exactly
 * the kind of privileged act where accepting them from a request body would be worst.
 */
data class MfaEnrollmentExemptionAdminGrantRequest(
    val userId: String,
    /** UTC; must be in the future and inside the configured ceiling. */
    val expiresAt: LocalDateTime,
    val reason: String,
)

data class MfaEnrollmentExemptionAdminRevokeRequest(
    val userId: String,
    val reason: String,
)

data class MfaEnrollmentExemptionAdminResponse(
    val userId: String,
    val status: String,
    val reason: String,
    val grantedBy: String,
    val grantedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val revokedBy: String?,
    val revokeReason: String?,
    val revokedAt: LocalDateTime?,
)
