package io.kudos.ms.auth.provider.emailotp.store

import java.time.Instant

/** Contains digests only; neither raw email nor raw OTP may enter the challenge store. */
data class EmailOtpChallenge(
    val transactionId: String,
    val tenantDigest: String,
    val emailDigest: String,
    val codeDigest: String,
    val expiresAt: Instant,
    val maxAttempts: Int,
)

enum class EmailOtpVerificationResult {
    VERIFIED,
    NOT_FOUND,
    EXPIRED,
    MISMATCH,
    ATTEMPTS_EXHAUSTED,
}

interface IEmailOtpChallengeStore {
    fun issue(challenge: EmailOtpChallenge): Boolean

    /** Atomically verifies, consumes on success and increments the bounded failure count on mismatch. */
    fun verify(
        transactionId: String,
        tenantDigest: String,
        emailDigest: String,
        codeDigest: String,
        now: Instant,
    ): EmailOtpVerificationResult

    fun cancel(transactionId: String)
}
