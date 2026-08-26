package io.kudos.ms.auth.common.authentication.vo

/** Stable error contract for TOTP enrollment and removal operations. */
data class TotpEnrollmentError(
    val success: Boolean = false,
    val code: String,
    val message: String,
)
