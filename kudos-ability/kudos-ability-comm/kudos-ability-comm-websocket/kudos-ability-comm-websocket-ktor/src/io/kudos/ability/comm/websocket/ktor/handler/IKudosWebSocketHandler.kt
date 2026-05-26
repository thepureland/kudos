package io.kudos.ability.comm.websocket.ktor.handler

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession

/**
 * Business-side WebSocket handling SPI.
 *
 * Classes implementing this interface are responsible for:
 *  - Accepting new connections ([onConnect]) — typically used for authentication, attaching
 *    context, pushing a welcome message, etc.
 *  - Handling each text / binary frame ([onText] / [onBinary]).
 *  - Cleanup ([onDisconnect]) — releasing resources held on the business side.
 *
 * Implementations **do not** need to explicitly manage [KudosWebSocketSession] registration
 * in [io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry] — the upstream
 * route extension function (see the `kudosWebSocket` implementation) already drives this
 * interface in a "register → handle → unregister" template.
 *
 * All interface default implementations are no-ops; the business side overrides as needed.
 *
 * @author K
 * @since 1.0.0
 */
interface IKudosWebSocketHandler {

    /** Invoked after the connection is established and before the first business frame. */
    suspend fun onConnect(session: KudosWebSocketSession) {}

    /** Called when a text frame is received. `text` is the already UTF-8-decoded content. */
    suspend fun onText(session: KudosWebSocketSession, text: String) {}

    /** Called when a binary frame is received. */
    suspend fun onBinary(session: KudosWebSocketSession, bytes: ByteArray) {}

    /**
     * Connection closed (normally or abnormally). A non-null `cause` indicates an abnormal
     * disconnect — the business side may use it to decide whether to clear a persisted
     * "online status" flag. **Before** this method returns the registry still holds the
     * session; afterwards it is automatically unregistered, so `session.sendXxx` may still be
     * called here but will very likely fail (the connection is unreachable). The business
     * side should not rely on it.
     */
    suspend fun onDisconnect(session: KudosWebSocketSession, cause: Throwable? = null) {}
}
