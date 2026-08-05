package io.kudos.ability.security.enforcement.resilience

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.base.logger.LogFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Stops the enforcement path from paying the storage timeout on every request once the decision
 * point has clearly stopped answering.
 *
 * **The measurement behind it.** Against a stopped database every enforced request sat for the
 * datasource's full connection timeout — 30 seconds — and then refused. Refusing was correct;
 * taking 30 seconds to refuse was not: request threads are a fixed pool, so a storage outage turns
 * into unavailability of everything else the service does.
 *
 * **Why this runs on the caller's thread, and what that costs.** A previous attempt moved the work
 * to a pool so it could be timed out, and that was a mistake serious enough to be worth recording:
 * subject resolution reads request-bound `ThreadLocal` state, which does not exist on a pool
 * thread, so every authorized request was refused. A guard that denies everyone is not a safer
 * guard — it is an outage. **You cannot relocate work that depends on where it is running**, and
 * you cannot interrupt a blocking call from the thread that is blocked on it.
 *
 * So the per-call bound is *not* this class's job and cannot be: it belongs where the blocking
 * happens, as a datasource connection/socket timeout. What is achievable here — and what actually
 * removes the pile-up — is refusing to make the call again once it is known to fail. During an
 * outage a handful of requests pay the storage timeout, the breaker opens, and the rest are refused
 * immediately without touching storage at all.
 *
 * **Every failure denies.** An exception, or an open breaker, yields the caller's refusal value.
 * Nothing here may let a request through because the system could not work out whether it should.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class DecisionGuard(
    private val properties: EnforcementProperties.ResilienceProperties,
) {

    private val log = LogFactory.getLog(this::class)

    private val consecutiveFailures = AtomicInteger(0)

    /** Epoch millis until which the breaker refuses outright; 0 means closed. */
    private val openUntil = AtomicLong(0)

    /**
     * Runs [block] on the calling thread, refusing without calling it while the breaker is open.
     *
     * @param what named in the logs, so an operator can tell which step is failing
     * @param onUnavailable the refusal to return when the decision cannot be made
     * @param block the decision itself
     */
    open fun <T> guard(what: String, onUnavailable: () -> T, block: () -> T): T {
        if (!properties.enabled) return block()

        if (isOpen()) {
            log.warn("[permission-enforcement][resilience] refusing ${what} unasked: the breaker is open.")
            return onUnavailable()
        }
        return try {
            val result = block()
            recordSuccess()
            result
        } catch (e: Exception) {
            recordFailure()
            log.warn("[permission-enforcement][resilience] refusing ${what}: the decision failed.", e)
            onUnavailable()
        }
    }

    /**
     * Expiry moves the breaker to *half-open* rather than straight to closed: the next call runs for
     * real and its outcome decides whether the breaker shuts or re-opens. Priming the counter one
     * short of the threshold is what makes a single failed probe re-open it, and a single success
     * close it.
     */
    private fun isOpen(): Boolean {
        val until = openUntil.get()
        if (until == 0L) return false
        if (System.currentTimeMillis() < until) return true
        if (openUntil.compareAndSet(until, 0L)) {
            consecutiveFailures.set(properties.failureThreshold - 1)
            log.info("[permission-enforcement][resilience] breaker half-open; the next decision is a probe.")
        }
        return false
    }

    private fun recordSuccess() {
        if (consecutiveFailures.getAndSet(0) >= properties.failureThreshold) {
            log.info("[permission-enforcement][resilience] breaker closed; decisions are being served again.")
        }
    }

    private fun recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= properties.failureThreshold) {
            val until = System.currentTimeMillis() + properties.openMillis
            if (openUntil.getAndSet(until) == 0L) {
                log.error(
                    "[permission-enforcement][resilience] breaker OPEN for ${properties.openMillis}ms after " +
                        "${properties.failureThreshold} consecutive failures — requests will be refused " +
                        "without touching the decision point until it recovers.",
                )
            }
        }
    }
}
