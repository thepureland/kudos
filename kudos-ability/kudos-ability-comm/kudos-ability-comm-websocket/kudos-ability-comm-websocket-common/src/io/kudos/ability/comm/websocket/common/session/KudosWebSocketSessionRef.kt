package io.kudos.ability.comm.websocket.common.session


/**
 * Session **abstraction** used by the registry / broadcaster.
 *
 * The purpose of extracting this interface is to keep business-layer components such as
 * [KudosWebSocketRegistry],
 * [io.kudos.ability.comm.websocket.common.broadcast.WebSocketBroadcaster] and
 * [io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler] from naming any
 * engine's own session type:
 *
 *  - **Testability**: unit tests can implement this interface with a plain data object,
 *    without starting a server or mocking a WebSocket context.
 *  - **Multi-engine compatibility**: the registry, broadcaster, handler SPI and admission
 *    interceptors in this module are shared verbatim by every engine module; adding one means
 *    contributing a [KudosWebSocketSessionRef] implementation and a route/endpoint adapter,
 *    nothing more.
 *
 * The engine modules supply the real implementations —
 * `io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession` wraps Ktor's
 * `DefaultWebSocketServerSession`, and
 * `io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession` wraps Spring's
 * `org.springframework.web.socket.WebSocketSession`. Both keep the native session reachable as
 * `raw` for code that genuinely needs it.
 *
 * Note that [close] takes a [WebSocketCloseReason] rather than an engine close type: that is the
 * one place where an engine-neutral abstraction would otherwise leak, and it is why this interface
 * can live in a module that depends on neither Ktor nor Spring WebSocket.
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

    /**
     * Open extension point (client version, device ID, Locale, etc.).
     *
     * Values are non-null: the backing map is a `ConcurrentHashMap`, which rejects null values
     * outright. Remove the key instead of storing null.
     */
    val attributes: MutableMap<String, Any>

    /** Sends a text frame. */
    suspend fun sendText(text: String)

    /** Sends a binary frame. */
    suspend fun sendBinary(bytes: ByteArray)

    /** Sends a [WebSocketPayload], dispatching to [sendText] / [sendBinary] by payload kind. */
    suspend fun send(payload: WebSocketPayload) {
        when (payload) {
            is WebSocketPayload.Text -> sendText(payload.text)
            is WebSocketPayload.Binary -> sendBinary(payload.bytes)
        }
    }

    /** Closes the connection normally. */
    suspend fun close(reason: WebSocketCloseReason = WebSocketCloseReason(WebSocketCloseReason.Codes.NORMAL, ""))
}
