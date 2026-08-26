package io.kudos.ms.auth.common.authentication.vo

import java.time.Instant

/** Newly generated recovery codes. Raw codes are returned once and cannot be read again. */
data class RecoveryCodeSet(
    val codes: List<String>,
    val generatedAt: Instant,
)
