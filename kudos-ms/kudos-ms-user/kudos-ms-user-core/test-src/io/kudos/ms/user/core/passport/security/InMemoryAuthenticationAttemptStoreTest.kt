package io.kudos.ms.user.core.passport.security

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class InMemoryAuthenticationAttemptStoreTest {

    @Test
    fun multipleBucketsAreCheckedBeforeAnyCounterIsConsumed() {
        val store = InMemoryAuthenticationAttemptStore()
        val strict = AuthenticationAttemptBucket("strict", 1, 60)
        val relaxed = AuthenticationAttemptBucket("relaxed", 2, 60)

        assertTrue(store.consume(listOf(strict, relaxed)).allowed)
        assertFalse(store.consume(listOf(strict, relaxed)).allowed)

        store.clear(listOf("strict"))
        assertTrue(store.consume(listOf(strict, relaxed)).allowed)
        assertFalse(store.inspect(listOf(relaxed)).allowed)
    }

    @Test
    fun windowExpiryAllowsTrafficAgainAndReportsRetryDelay() {
        val clock = MutableClock(Instant.parse("2026-08-24T00:00:00Z"))
        val store = InMemoryAuthenticationAttemptStore(clock)
        val bucket = AuthenticationAttemptBucket("principal", 1, 10)

        assertTrue(store.consume(listOf(bucket)).allowed)
        val blocked = store.consume(listOf(bucket))
        assertFalse(blocked.allowed)
        assertEquals(10, blocked.retryAfterSeconds)

        clock.advanceSeconds(10)
        assertTrue(store.consume(listOf(bucket)).allowed)
    }

    private class MutableClock(private var current: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = current
        fun advanceSeconds(seconds: Long) {
            current = current.plusSeconds(seconds)
        }
    }
}
