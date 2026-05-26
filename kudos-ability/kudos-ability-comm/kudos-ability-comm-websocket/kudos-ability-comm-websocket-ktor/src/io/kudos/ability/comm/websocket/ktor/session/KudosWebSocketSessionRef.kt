package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.websocket.CloseReason

/**
 * Session **abstraction** used by the registry / broadcaster.
 *
 * The purpose of extracting this interface is to keep business-layer components such as
 * [KudosWebSocketRegistry] and
 * [io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster] from directly
 * depending on Ktor's `DefaultWebSocketServerSession`:
 *
 *  - **Testability**: unit tests can implement this interface with a plain data object
 *    without starting Ktor or mocking a WebSocket context.
 *  - **Multi-engine compatibility**: if a future non-Ktor implementation (e.g. raw netty) is
 *    added, the same registry / broadcast abstractions can be reused — only a new
 *    [KudosWebSocketSessionRef] implementation is needed.
 *
 * At runtime, [KudosWebSocketSession] is the real implementation — it implements this
 * interface and also holds the `raw` Ktor session.
 *
 * @author K
 * @since 1.0.0
 */
interface KudosWebSocketSessionRef {
    /** Unique identifier within the process. */
    val sessionId: String

    /** Populated by the business side when the connection is established; null means an anonymous session. */
    val userId: String?

    /** Populated by the business side when the connection is established; isolation key for multi-tenant scenarios. */
    val tenantId: String?

    /** Open extension point (client version, device ID, Locale, etc.). */
    val attributes: MutableMap<String, Any?>

    /** Sends a text frame. */
    suspend fun sendText(text: String)

    /** Sends a binary frame. */
    suspend fun sendBinary(bytes: ByteArray)

    /** Closes the connection normally. */
    suspend fun close(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""))
}
