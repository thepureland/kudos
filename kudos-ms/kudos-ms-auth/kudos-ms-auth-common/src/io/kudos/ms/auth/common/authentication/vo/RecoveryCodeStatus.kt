package io.kudos.ms.auth.common.authentication.vo

/** Secret-free status for the current recovery-code set. */
data class RecoveryCodeStatus(
    val enabled: Boolean,
    val remaining: Int,
)
