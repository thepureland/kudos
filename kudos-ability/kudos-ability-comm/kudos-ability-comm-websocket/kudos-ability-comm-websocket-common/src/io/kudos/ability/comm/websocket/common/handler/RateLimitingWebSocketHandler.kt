package io.kudos.ability.comm.websocket.common.handler

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.base.logger.LogFactory

/**
 * Per-session inbound rate limit, as a [WebSocketHandlerDecorator].
 *
 * A token bucket per connection: [permitsPerSecond] sustained messages, absorbing bursts up to
 * [burst]. Messages that cannot get a token are dropped before reaching the delegate.
 *
 * The bucket lives in [KudosWebSocketSessionRef.attributes], which means it is created lazily per
 * connection and disappears with it — no global map keyed by sessionId to grow, and no eviction
 * policy to get wrong. This is what the `attributes` extension point is for.
 *
 * Dropping rather than closing is the default because a burst above the limit is usually a chatty
 * client, not an attacker; override [onLimitExceeded] to close the session, reply with an error
 * frame, or increment a metric instead.
 *
 * ```kotlin
 * val handler = chatHandler.wrappedBy({ next -> RateLimitingWebSocketHandler(next, permitsPerSecond = 10.0) })
 * ```
 *
 * @param delegate the next handler in the chain
 * @param permitsPerSecond sustained message rate allowed per session
 * @param burst bucket capacity — how many messages an idle session may send back to back
 * @param nanoTime time source; injectable so tests do not have to sleep
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class RateLimitingWebSocketHandler(
    delegate: IKudosWebSocketHandler,
    private val permitsPerSecond: Double = DEFAULT_PERMITS_PER_SECOND,
    private val burst: Int = DEFAULT_BURST,
    private val nanoTime: () -> Long = System::nanoTime,
) : WebSocketHandlerDecorator(delegate) {

    private val log = LogFactory.getLog(this::class)

    init {
        require(permitsPerSecond > 0) { "permitsPerSecond must be positive, got $permitsPerSecond" }
        require(burst > 0) { "burst must be positive, got $burst" }
    }

    override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
        if (acquire(session)) super.onText(session, text) else onLimitExceeded(session)
    }

    override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {
        if (acquire(session)) super.onBinary(session, bytes) else onLimitExceeded(session)
    }

    /**
     * Called instead of the delegate when a message is rejected. Default: log, dropping the message.
     *
     * Only the *first* rejection of a run is logged at WARN — a limiter that logged every rejected
     * message would turn a flood into a log flood, doing the caller's work for them. Subsequent
     * rejections stay at DEBUG until the session gets a token again.
     */
    protected open suspend fun onLimitExceeded(session: KudosWebSocketSessionRef) {
        val firstOfRun = bucketOf(session).markRejected()
        if (firstOfRun) {
            log.warn("Rate limit exceeded, dropping inbound messages sessionId={0} userId={1} limit={2}/s burst={3}",
                session.sessionId, session.userId, permitsPerSecond, burst)
        } else {
            log.debug("Rate limit still exceeded, dropping message sessionId={0}", session.sessionId)
        }
    }

    private fun acquire(session: KudosWebSocketSessionRef): Boolean =
        bucketOf(session).tryAcquire(permitsPerSecond, burst.toDouble(), nanoTime())

    private fun bucketOf(session: KudosWebSocketSessionRef): TokenBucket =
        session.attributes.computeIfAbsent(ATTRIBUTE_KEY) { TokenBucket(burst.toDouble(), nanoTime()) } as TokenBucket

    /**
     * Classic token bucket. Synchronized because although Ktor drives one session's frames from a
     * single coroutine, nothing in the SPI promises that — and a decorator is cheap to get wrong in
     * a way that only shows up under load.
     */
    private class TokenBucket(private var tokens: Double, private var lastRefillNanos: Long) {

        private var rejecting = false

        @Synchronized
        fun tryAcquire(permitsPerSecond: Double, capacity: Double, now: Long): Boolean {
            val elapsedNanos = now - lastRefillNanos
            if (elapsedNanos > 0) {
                tokens = minOf(capacity, tokens + elapsedNanos / NANOS_PER_SECOND * permitsPerSecond)
                lastRefillNanos = now
            }
            if (tokens < 1.0) return false
            tokens -= 1.0
            rejecting = false
            return true
        }

        /** @return true when this starts a new run of rejections (worth a WARN), false while one is ongoing */
        @Synchronized
        fun markRejected(): Boolean {
            if (rejecting) return false
            rejecting = true
            return true
        }
    }

    companion object {
        /** Attribute key holding this connection's token bucket. */
        const val ATTRIBUTE_KEY: String = "_KUDOS_WS_RATE_LIMIT_BUCKET_"

        /** Comfortable for chat / telemetry style traffic; raise for high-frequency streams. */
        const val DEFAULT_PERMITS_PER_SECOND: Double = 20.0

        /** Lets an idle client send a short burst without being penalized. */
        const val DEFAULT_BURST: Int = 40

        private const val NANOS_PER_SECOND: Double = 1_000_000_000.0
    }
}
