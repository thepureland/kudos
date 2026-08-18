package io.kudos.ability.comm.websocket.common.handler

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef

/**
 * Business-side WebSocket handling SPI.
 *
 * Classes implementing this interface are responsible for:
 *  - Accepting new connections ([onConnect]) — typically used for attaching context, pushing a
 *    welcome message, etc.
 *  - Handling each text / binary message ([onText] / [onBinary]).
 *  - Cleanup ([onDisconnect]) — releasing resources held on the business side.
 *
 * Hooks receive a [KudosWebSocketSessionRef], not the concrete Ktor-backed session: an
 * implementation can then be unit-tested against a plain stub, and survives a future non-Ktor
 * engine unchanged. Handlers that genuinely need the raw Ktor session can cast to
 * [io.kudos.ability.comm.websocket.common.session.KudosWebSocketSession].
 *
 * Implementations **do not** need to explicitly manage registration in
 * [io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry] — the upstream route
 * extension function (see the `kudosWebSocket` implementation) already drives this interface in
 * a "register → handle → unregister" template.
 *
 * All interface default implementations are no-ops; the business side overrides as needed.
 *
 * @author K
 * @since 1.0.0
 */
interface IKudosWebSocketHandler {

    /** Invoked after the connection is established and before the first business message. */
    suspend fun onConnect(session: KudosWebSocketSessionRef) {}

    /**
     * Called for each complete text message. `text` is the already UTF-8-decoded content, with
     * WebSocket fragmentation already reassembled — one call per application message, never per
     * wire frame.
     */
    suspend fun onText(session: KudosWebSocketSessionRef, text: String) {}

    /** Called for each complete binary message (fragmentation already reassembled). */
    suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {}

    /**
     * Connection closed (normally or abnormally). A non-null `cause` indicates an abnormal
     * disconnect — the business side may use it to decide whether to clear a persisted
     * "online status" flag. A `cause` of [kotlinx.coroutines.CancellationException] specifically
     * means the server is shutting down or the route coroutine was cancelled.
     *
     * **Before** this method returns the registry still holds the session; afterwards it is
     * automatically unregistered, so `session.sendXxx` may still be called here but will very
     * likely fail (the connection is unreachable). The business side should not rely on it.
     *
     * Runs inside [kotlinx.coroutines.NonCancellable], so cleanup that suspends (clearing a Redis
     * presence key, writing an audit row) still completes during a graceful shutdown.
     */
    suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable? = null) {}
}
