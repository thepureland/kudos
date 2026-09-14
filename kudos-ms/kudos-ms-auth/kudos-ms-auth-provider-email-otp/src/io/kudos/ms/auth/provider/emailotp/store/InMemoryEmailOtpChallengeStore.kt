package io.kudos.ms.auth.provider.emailotp.store

import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Single-node fallback with the same one-consume semantics as the Redis implementation. */
open class InMemoryEmailOtpChallengeStore : IEmailOtpChallengeStore {
    private data class Entry(val challenge: EmailOtpChallenge, val failures: Int = 0)

    private val entries = ConcurrentHashMap<String, Entry>()

    override fun issue(challenge: EmailOtpChallenge): Boolean =
        entries.putIfAbsent(challenge.transactionId, Entry(challenge)) == null

    override fun verify(
        transactionId: String,
        tenantDigest: String,
        emailDigest: String,
        codeDigest: String,
        now: Instant,
    ): EmailOtpVerificationResult {
        var result = EmailOtpVerificationResult.NOT_FOUND
        entries.compute(transactionId) { _, current ->
            if (current == null) return@compute null
            val challenge = current.challenge
            when {
                !now.isBefore(challenge.expiresAt) -> {
                    result = EmailOtpVerificationResult.EXPIRED
                    null
                }

                current.failures >= challenge.maxAttempts -> {
                    result = EmailOtpVerificationResult.ATTEMPTS_EXHAUSTED
                    null
                }

                !constantTimeEquals(challenge.tenantDigest, tenantDigest) ||
                    !constantTimeEquals(challenge.emailDigest, emailDigest) ||
                    !constantTimeEquals(challenge.codeDigest, codeDigest) -> {
                    val failures = current.failures + 1
                    result = if (failures >= challenge.maxAttempts) {
                        EmailOtpVerificationResult.ATTEMPTS_EXHAUSTED
                    } else {
                        EmailOtpVerificationResult.MISMATCH
                    }
                    if (failures >= challenge.maxAttempts) null else current.copy(failures = failures)
                }

                else -> {
                    result = EmailOtpVerificationResult.VERIFIED
                    null
                }
            }
        }
        return result
    }

    override fun cancel(transactionId: String) {
        entries.remove(transactionId)
    }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(Charsets.US_ASCII), right.toByteArray(Charsets.US_ASCII))
}
