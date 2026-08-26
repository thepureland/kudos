package io.kudos.ms.auth.common.authentication.vo

/** Public, secret-free TOTP enrollment status for the current user. */
data class TotpEnrollmentStatus(
    val enabled: Boolean,
)
