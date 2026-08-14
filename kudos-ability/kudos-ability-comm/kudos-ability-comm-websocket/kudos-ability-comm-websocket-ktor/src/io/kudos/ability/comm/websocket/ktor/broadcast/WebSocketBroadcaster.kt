package io.kudos.ability.comm.websocket.ktor.broadcast

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.ktor.session.WebSocketPayload
import io.kudos.base.logger.LogFactory
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Process-local multi-target broadcast / unicast utility.
 *
 * Looks receivers up in [KudosWebSocketRegistry] and pushes to them concurrently: each session's
 * send runs in its own coroutine, and a failure on one session is caught, logged at WARN and
 * counted as a miss without stopping the others.
 *
 * ## Bounding a fan-out
 *
 * Two knobs keep a large or unhealthy fan-out from taking the process down:
 *
 *  - [maxConcurrentSends] caps how many sends are in flight at once, across all concurrent
 *    broadcast calls. Without it, broadcasting to 100k sessions would start 100k coroutines and
 *    materialize 100k queued frames at once.
 *  - [sendTimeout] bounds how long one session may stall the fan-out. A client that has stopped
 *    reading does not fail its send — it silently accumulates frames in Ktor's outgoing channel
 *    until the process runs out of memory, and it holds a concurrency permit the whole time.
 *
 * When a send times out, [closeOnSendTimeout] decides the fate of the connection. It defaults to
 * closing, because a timeout without a close leaves the already-queued frames in memory: detecting
 * the condition and then doing nothing about it does not fix the leak it detects. Timing out is
 * safe with respect to the wire protocol — Ktor's `send` suspends until the frame is accepted by
 * the outgoing channel, and cancelling before that point means the frame was never serialized, so
 * no half-written frame can corrupt the stream.
 *
 * Retries are deliberately absent: retry policy is a business concern, and this class only
 * guarantees that a slow or broken session does not hold up the rest.
 *
 * @param registry source of truth for who is connected
 * @param sendTimeout per-session send deadline
 * @param maxConcurrentSends upper bound on in-flight sends for this broadcaster
 * @param closeOnSendTimeout close a session whose send timed out
 * @author K
 * @since 1.0.0
 */
class WebSocketBroadcaster(
    private val registry: KudosWebSocketRegistry,
    private val sendTimeout: Duration = DEFAULT_SEND_TIMEOUT,
    maxConcurrentSends: Int = DEFAULT_MAX_CONCURRENT_SENDS,
    private val closeOnSendTimeout: Boolean = true,
) : IWebSocketBroadcaster {

    private val log = LogFactory.getLog(this::class)

    private val permits = Semaphore(maxConcurrentSends)

    override suspend fun broadcast(payload: WebSocketPayload): Int = sendTo(registry.all(), payload)

    override suspend fun broadcastToUser(userId: String, payload: WebSocketPayload): Int =
        sendTo(registry.findByUserId(userId), payload)

    override suspend fun broadcastToTenant(tenantId: String, payload: WebSocketPayload): Int =
        sendTo(registry.findByTenantId(tenantId), payload)

    override suspend fun unicast(sessionId: String, payload: WebSocketPayload): Boolean {
        val session = registry.findById(sessionId) ?: return false
        return sendOne(session, payload)
    }

    private suspend fun sendTo(sessions: List<KudosWebSocketSessionRef>, payload: WebSocketPayload): Int {
        if (sessions.isEmpty()) return 0
        return coroutineScope {
            sessions.map { session -> async { if (sendOne(session, payload)) 1 else 0 } }.awaitAll().sum()
        }
    }

    /** Sends to one session under the concurrency permit and the send deadline. Never throws except on caller cancellation. */
    private suspend fun sendOne(session: KudosWebSocketSessionRef, payload: WebSocketPayload): Boolean =
        permits.withPermit {
            try {
                withTimeout(sendTimeout) { session.send(payload) }
                true
            } catch (e: TimeoutCancellationException) {
                onSendTimeout(session, e)
                false
            } catch (t: Throwable) {
                // The caller being cancelled is not a delivery failure — let it unwind.
                if (t is CancellationException) throw t
                log.warn("Send to sessionId={0} failed cause={1}", session.sessionId, t.message)
                false
            }
        }

    private suspend fun onSendTimeout(session: KudosWebSocketSessionRef, cause: TimeoutCancellationException) {
        log.warn("Send to sessionId={0} timed out after {1}; the client is not draining its socket. cause={2}",
            session.sessionId, sendTimeout, cause.message)
        if (!closeOnSendTimeout) return
        runCatching { session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "send timeout")) }
            .onFailure { log.warn("Closing timed-out sessionId={0} failed cause={1}", session.sessionId, it.message) }
    }

    companion object {
        /** Generous enough that a healthy client never hits it; short enough that a stuck one is reclaimed. */
        val DEFAULT_SEND_TIMEOUT: Duration = 10.seconds

        /** In-flight send cap. Large enough to saturate a NIC, small enough to bound queued frames. */
        const val DEFAULT_MAX_CONCURRENT_SENDS: Int = 512
    }
}
