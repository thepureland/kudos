package io.kudos.ms.auth.common.authentication.vo

/** Stable public error contract for recovery-code management. */
data class RecoveryCodeError(
    val success: Boolean = false,
    val code: String,
    val message: String,
)
