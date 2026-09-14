package io.kudos.ms.auth.provider.emailotp.store

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class InMemoryEmailOtpChallengeStoreTest {
    @Test
    fun `challenge is single issue single consume and bounded attempts`() {
        val store = InMemoryEmailOtpChallengeStore()
        val now = Instant.parse("2026-08-28T00:00:00Z")
        val challenge = EmailOtpChallenge("tx", "tenant", "email", "code", now.plusSeconds(60), 2)

        assertTrue(store.issue(challenge))
        assertFalse(store.issue(challenge.copy(codeDigest = "attacker")))
        assertEquals(
            EmailOtpVerificationResult.MISMATCH,
            store.verify("tx", "tenant", "email", "wrong", now),
        )
        assertEquals(
            EmailOtpVerificationResult.VERIFIED,
            store.verify("tx", "tenant", "email", "code", now),
        )
        assertEquals(
            EmailOtpVerificationResult.NOT_FOUND,
            store.verify("tx", "tenant", "email", "code", now),
        )

        assertTrue(store.issue(challenge.copy(transactionId = "locked")))
        assertEquals(
            EmailOtpVerificationResult.MISMATCH,
            store.verify("locked", "tenant", "email", "wrong", now),
        )
        assertEquals(
            EmailOtpVerificationResult.ATTEMPTS_EXHAUSTED,
            store.verify("locked", "tenant", "email", "wrong", now),
        )
        assertEquals(
            EmailOtpVerificationResult.NOT_FOUND,
            store.verify("locked", "tenant", "email", "code", now),
        )
    }

    @Test
    fun `expired challenge is removed`() {
        val store = InMemoryEmailOtpChallengeStore()
        val now = Instant.parse("2026-08-28T00:00:00Z")
        assertTrue(store.issue(EmailOtpChallenge("tx", "tenant", "email", "code", now, 3)))

        assertEquals(
            EmailOtpVerificationResult.EXPIRED,
            store.verify("tx", "tenant", "email", "code", now),
        )
        assertEquals(
            EmailOtpVerificationResult.NOT_FOUND,
            store.verify("tx", "tenant", "email", "code", now),
        )
    }
}
