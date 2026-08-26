package io.kudos.ms.auth.common.authentication.vo

import java.time.Instant

/** One-time TOTP enrollment material; the secret is returned only when enrollment begins. */
data class TotpEnrollmentChallenge(
    val enrollmentId: String,
    val secret: String,
    val otpauthUrl: String,
    val expiresAt: Instant,
)
