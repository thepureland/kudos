package io.kudos.ability.security.enforcement.resilience

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * junit test for [DecisionGuard].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DecisionGuardTest {

    private fun props(configure: EnforcementProperties.ResilienceProperties.() -> Unit = {}) =
        EnforcementProperties.ResilienceProperties().apply {
            failureThreshold = 2
            openMillis = 300
        }.apply(configure)

    /**
     * The regression that made this class necessary, and the one its predecessor's tests could not
     * see.
     *
     * The earlier guard ran the decision on a pool thread so it could be timed out. Subject
     * resolution reads request-bound `ThreadLocal` state, which does not exist there — so with the
     * guard enabled by default, **every authorized request was refused**. The unit tests all passed:
     * their fake providers had no thread affinity, so nothing noticed. Only an authorized request
     * against a running server did, and only because it was finally checked.
     *
     * Hence this assertion, which is about *where* the work runs rather than what it returns.
     */
    @Test
    fun theDecisionRunsOnTheCallersThread() {
        val caller = Thread.currentThread()
        var observed: Thread? = null
        DecisionGuard(props()).guard("x", onUnavailable = { false }) {
            observed = Thread.currentThread()
            true
        }
        assertSame(caller, observed, "the decision must not be relocated; it reads request-bound state")
    }

    /** Request-bound state must survive the guard, which is the practical form of the above. */
    @Test
    fun threadLocalStateIsVisibleInsideTheGuard() {
        val local = ThreadLocal<String>()
        local.set("subject-42")
        try {
            val seen = DecisionGuard(props()).guard("x", onUnavailable = { null }) { local.get() }
            assertEquals("subject-42", seen)
        } finally {
            local.remove()
        }
    }

    @Test
    fun aHealthyDecisionPassesThroughUnchanged() {
        assertTrue(DecisionGuard(props()).guard("x", onUnavailable = { false }) { true })
    }

    /** A failure denies. Never let a request through because the system could not decide. */
    @Test
    fun aFailingDecisionYieldsTheRefusal() {
        val allowed = DecisionGuard(props()).guard<Boolean>("x", onUnavailable = { false }) {
            throw IllegalStateException("decision backend unavailable")
        }
        assertTrue(!allowed)
    }

    /**
     * The point of the whole class: once the backend is known to be down, stop calling it. That is
     * what removes the per-request storage timeout, which was measured at 30 seconds.
     */
    @Test
    fun onceOpenTheBreakerStopsCallingTheDecisionEntirely() {
        val guard = DecisionGuard(props())
        var calls = 0
        fun attempt() = guard.guard<Boolean>("x", onUnavailable = { false }) {
            calls++
            throw IllegalStateException("still down")
        }

        repeat(2) { attempt() }
        val callsWhenOpened = calls

        repeat(5) { assertTrue(!attempt()) }
        assertEquals(callsWhenOpened, calls, "an open breaker must not touch the decision point")
    }

    /** ...and it recovers by itself: one successful probe closes it again. */
    @Test
    fun theBreakerRecoversAfterTheBackendDoes() {
        val guard = DecisionGuard(props())
        var healthy = false
        fun attempt() = guard.guard<Boolean>("x", onUnavailable = { false }) {
            if (!healthy) throw IllegalStateException("still down")
            true
        }

        repeat(2) { attempt() }
        assertTrue(!attempt(), "the breaker should be open")

        healthy = true
        Thread.sleep(400) // outlast openMillis

        assertTrue(attempt(), "the probe should succeed and close the breaker")
        assertTrue(attempt(), "and it should stay closed")
    }

    /** A failed probe must re-open rather than let the next few requests through. */
    @Test
    fun aFailedProbeReOpensImmediately() {
        val guard = DecisionGuard(props())
        var calls = 0
        fun attempt() = guard.guard<Boolean>("x", onUnavailable = { false }) {
            calls++
            throw IllegalStateException("still down")
        }

        repeat(2) { attempt() }
        Thread.sleep(400) // half-open
        attempt() // the probe, which fails
        val callsAfterProbe = calls

        repeat(3) { attempt() }
        assertEquals(callsAfterProbe, calls, "a failed probe must re-open the breaker at once")
    }

    /**
     * Switched off, it must be a plain call — the failure propagates exactly as it did before this
     * class existed, rather than being converted into a refusal. Turning the guard off has to
     * restore the previous behaviour completely, or "off" is its own third behaviour.
     */
    @Test
    fun disabledIsAPlainPassThrough() {
        val guard = DecisionGuard(props { enabled = false })
        var calls = 0
        repeat(5) {
            assertFailsWith<IllegalStateException> {
                guard.guard<Boolean>("x", onUnavailable = { false }) {
                    calls++
                    throw IllegalStateException("down")
                }
            }
        }
        assertEquals(5, calls, "with the breaker off, every call must still be attempted")
    }
}
