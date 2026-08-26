package io.kudos.ms.auth.common.authentication.vo.request

/** Current six-digit code from the authenticator app used to confirm pending enrollment. */
data class ConfirmTotpEnrollmentRequest(
    val code: Int,
)
