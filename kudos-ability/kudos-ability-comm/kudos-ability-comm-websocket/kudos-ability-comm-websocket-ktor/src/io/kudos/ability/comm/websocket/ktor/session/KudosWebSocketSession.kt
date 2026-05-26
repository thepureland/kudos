package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Kudos business-layer wrapper around a WebSocket session.
 *
 * Attaches the metadata required by the business side on top of the native
 * [DefaultWebSocketServerSession]:
 *  - [sessionId]: unique session identifier within the process (defaults to a UUID)
 *  - [userId] / [tenantId]: populated by the business side when the connection is established;
 *    used for indexing and broadcasting by business dimension
 *  - [attributes]: open extension point (client version, device ID, Locale, etc.)
 *
 * Upstream route code holds a [KudosWebSocketSession] rather than the Ktor session directly.
 * Benefits:
 *  - Unified close/send semantics ([send] / [close]); swapping the underlying engine later
 *    does not require changing business code.
 *  - Multiple business modules share the same object, avoiding each one maintaining its own
 *    "sessionId → user" mapping.
 *
 * Thread-safety: a single session has its frames processed by a single Ktor coroutine; sends
 * from any coroutine enter Ktor's internal send channel, so this class adds no extra locking.
 * [attributes] uses a concurrent Map.
 *
 * @author K
 * @since 1.0.0
 */
class KudosWebSocketSession(
    val raw: DefaultWebSocketServerSession,
    override val sessionId: String = UUID.randomUUID().toString(),
    override val userId: String? = null,
    override val tenantId: String? = null,
) : KudosWebSocketSessionRef {
    override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()

    /** Sends a text frame. Thin wrapper over `raw.send(...)`. */
    override suspend fun sendText(text: String) {
        raw.send(Frame.Text(text))
    }

    /** Sends a binary frame. */
    override suspend fun sendBinary(bytes: ByteArray) {
        raw.send(Frame.Binary(true, bytes))
    }

    /**
     * Closes the connection normally. Defaults to `NORMAL` close reason; the business side may
     * explicitly pass `GOING_AWAY` etc. After close, the incoming channel of [raw] is closed
     * automatically and the corresponding route lambda exits.
     */
    override suspend fun close(reason: CloseReason) {
        raw.close(reason)
    }
}
