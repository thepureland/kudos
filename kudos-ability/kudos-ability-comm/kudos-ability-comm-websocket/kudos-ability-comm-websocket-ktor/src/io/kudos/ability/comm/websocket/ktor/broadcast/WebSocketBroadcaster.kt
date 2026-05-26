package io.kudos.ability.comm.websocket.ktor.broadcast

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.kudos.base.logger.LogFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Multi-target broadcast / unicast utility.
 *
 * Uses [KudosWebSocketRegistry] to look up receivers and then pushes messages to them
 * concurrently. "Concurrently" means each session's `sendText` is launched in parallel via
 * `async { }`; a send failure on any single session is caught and logged at WARN level
 * without stopping sends to the remaining sessions.
 *
 * Does **not** perform message batching or rate limiting — add these on the business side
 * when needed.
 *
 * @author K
 * @since 1.0.0
 */
class WebSocketBroadcaster(private val registry: KudosWebSocketRegistry) {

    private val log = LogFactory.getLog(this::class)

    /** Broadcasts to all connected sessions. */
    suspend fun broadcast(text: String): Int = sendTo(registry.all(), text)

    /** Broadcasts to all sessions of the given `userId` (multi-device online scenario). */
    suspend fun broadcastToUser(userId: String, text: String): Int =
        sendTo(registry.findByUserId(userId), text)

    /** Broadcasts to all sessions of the given `tenantId`. */
    suspend fun broadcastToTenant(tenantId: String, text: String): Int =
        sendTo(registry.findByTenantId(tenantId), text)

    /** Unicasts to the given sessionId. Returns whether the send succeeded. */
    suspend fun unicast(sessionId: String, text: String): Boolean {
        val session = registry.findById(sessionId) ?: return false
        return runCatching { session.sendText(text) }
            .onFailure { log.warn("Unicast failed sessionId={0} cause={1}", sessionId, it.message) }
            .isSuccess
    }

    private suspend fun sendTo(sessions: List<KudosWebSocketSessionRef>, text: String): Int {
        if (sessions.isEmpty()) return 0
        return coroutineScope {
            sessions.map { session ->
                async {
                    runCatching { session.sendText(text); 1 }.getOrElse { t ->
                        log.warn("Broadcast to sessionId={0} failed cause={1}", session.sessionId, t.message)
                        0
                    }
                }
            }.awaitAll().sum()
        }
    }
}
