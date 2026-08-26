package io.kudos.ms.user.core.passport.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DefaultAuthenticationAttemptLimiterTest {

    @Test
    fun requestLimitsApplyToPrincipalAndObservedIp() {
        val properties = properties().apply {
            principalMaxAttempts = 2
            ipMaxAttempts = 2
        }
        val limiter = DefaultAuthenticationAttemptLimiter(InMemoryAuthenticationAttemptStore(), properties)
        val context = context()

        assertTrue(limiter.consumeRequest(context).allowed)
        assertTrue(limiter.consumeRequest(context).allowed)
        val blocked = limiter.consumeRequest(context)
        assertFalse(blocked.allowed)
        assertEquals(AuthenticationAttemptDecisionReasonEnum.REQUEST_RATE_LIMIT, blocked.reason)
        assertEquals(60, blocked.retryAfterSeconds)
    }

    @Test
    fun passwordAndTotpFailureWindowsAreIndependentAndClearable() {
        val properties = properties().apply {
            passwordFailureMaxAttempts = 2
            totpFailureMaxAttempts = 1
        }
        val limiter = DefaultAuthenticationAttemptLimiter(InMemoryAuthenticationAttemptStore(), properties)
        val context = context()

        repeat(2) { limiter.recordFailure(context, AuthenticationAttemptFactorEnum.PASSWORD) }
        assertFalse(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.PASSWORD).allowed)
        assertTrue(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.TOTP).allowed)

        limiter.recordFailure(context, AuthenticationAttemptFactorEnum.TOTP)
        assertFalse(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.TOTP).allowed)
        limiter.clearFailures(context, setOf(AuthenticationAttemptFactorEnum.PASSWORD))
        assertTrue(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.PASSWORD).allowed)
        assertFalse(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.TOTP).allowed)
    }

    @Test
    fun counterKeysNeverContainTenantUsernameOrIp() {
        val store = CapturingStore()
        val limiter = DefaultAuthenticationAttemptLimiter(store, properties())

        limiter.consumeRequest(context())
        limiter.recordFailure(context(), AuthenticationAttemptFactorEnum.PASSWORD)

        assertTrue(store.keys.isNotEmpty())
        store.keys.forEach { key ->
            assertFalse("Tenant-A" in key)
            assertFalse("Alice" in key)
            assertFalse("2130706433" in key)
        }
    }

    @Test
    fun storeFailureIsClosedByDefaultAndMayBeExplicitlyOpened() {
        val closed = DefaultAuthenticationAttemptLimiter(ThrowingStore(), properties())
        val denied = closed.consumeRequest(context())
        assertFalse(denied.allowed)
        assertEquals(AuthenticationAttemptDecisionReasonEnum.INFRASTRUCTURE_UNAVAILABLE, denied.reason)

        val openProperties = properties().apply { failOpen = true }
        val open = DefaultAuthenticationAttemptLimiter(ThrowingStore(), openProperties)
        assertTrue(open.consumeRequest(context()).allowed)
    }

    private fun context() = AuthenticationAttemptContext("Tenant-A", "Alice", 2_130_706_433L)

    private fun properties() = AuthenticationAttemptLimitProperties().apply {
        principalWindowSeconds = 60
        ipWindowSeconds = 60
    }

    private class CapturingStore : IAuthenticationAttemptStore {
        val keys = mutableListOf<String>()
        override fun inspect(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision {
            keys += buckets.map { it.key }
            return AuthenticationAttemptStoreDecision.ALLOWED
        }

        override fun consume(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision {
            keys += buckets.map { it.key }
            return AuthenticationAttemptStoreDecision.ALLOWED
        }

        override fun clear(keys: Collection<String>) = Unit
    }

    private class ThrowingStore : IAuthenticationAttemptStore {
        override fun inspect(buckets: List<AuthenticationAttemptBucket>) = error("redis unavailable")
        override fun consume(buckets: List<AuthenticationAttemptBucket>) = error("redis unavailable")
        override fun clear(keys: Collection<String>) = error("redis unavailable")
    }
}
