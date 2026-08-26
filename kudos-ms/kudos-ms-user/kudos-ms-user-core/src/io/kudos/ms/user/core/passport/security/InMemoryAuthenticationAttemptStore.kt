package io.kudos.ms.user.core.passport.security

import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Single-node fixed-window store used only when the deployment has no Kudos Redis runtime. */
open class InMemoryAuthenticationAttemptStore(
    private val clock: Clock = Clock.systemUTC(),
) : IAuthenticationAttemptStore {
    private val lock = ReentrantLock()
    private val windows = HashMap<String, Window>()

    override fun inspect(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision =
        evaluate(buckets, consume = false)

    override fun consume(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision =
        evaluate(buckets, consume = true)

    override fun clear(keys: Collection<String>) {
        lock.withLock { keys.forEach(windows::remove) }
    }

    private fun evaluate(
        buckets: List<AuthenticationAttemptBucket>,
        consume: Boolean,
    ): AuthenticationAttemptStoreDecision = lock.withLock {
        if (buckets.isEmpty()) return AuthenticationAttemptStoreDecision.ALLOWED
        val now = clock.millis()
        buckets.forEach { bucket ->
            validate(bucket)
            windows[bucket.key]?.takeIf { it.expiresAtMillis <= now }?.let { windows.remove(bucket.key) }
        }
        val blockedUntil = buckets.mapNotNull { bucket ->
            windows[bucket.key]?.takeIf { it.count >= bucket.maxAttempts }?.expiresAtMillis
        }.maxOrNull()
        if (blockedUntil != null) {
            return AuthenticationAttemptStoreDecision(
                allowed = false,
                retryAfterSeconds = ((blockedUntil - now).coerceAtLeast(1) + 999) / 1000,
            )
        }
        if (consume) {
            buckets.forEach { bucket ->
                val current = windows[bucket.key]
                if (current == null) {
                    windows[bucket.key] = Window(1, now + bucket.windowSeconds * 1000)
                } else {
                    current.count++
                }
            }
        }
        AuthenticationAttemptStoreDecision.ALLOWED
    }

    private fun validate(bucket: AuthenticationAttemptBucket) {
        require(bucket.key.isNotBlank()) { "Authentication attempt key must not be blank" }
        require(bucket.maxAttempts in 1..MAX_ATTEMPTS) { "Invalid authentication max attempts" }
        require(bucket.windowSeconds in 1..MAX_WINDOW_SECONDS) { "Invalid authentication window" }
    }

    private data class Window(var count: Int, val expiresAtMillis: Long)

    private companion object {
        const val MAX_ATTEMPTS = 100_000
        const val MAX_WINDOW_SECONDS = 604_800L
    }
}
