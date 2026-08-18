package io.kudos.ability.comm.websocket.spring.handshake

import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest

/**
 * Pre-upgrade admission check: decides whether the HTTP request may become a WebSocket at all.
 *
 * This is the seam Ktor does not need an equivalent of — there, `authenticate("jwt") { ... }` wraps
 * the route with the framework's own plugin. Spring's WebSocket endpoints are registered through a
 * `WebSocketConfigurer` rather than through the MVC handler chain, so this module provides the hook
 * explicitly.
 *
 * ## Why not just reject in the session factory
 *
 * Because of what the client sees. A guard runs while the exchange is still plain HTTP, so a
 * rejection is an ordinary **401 / 403 response** the client can read, log and act on.
 * [io.kudos.ability.comm.websocket.spring.handler.ISpringWebSocketSessionFactory] returning `null`
 * runs *after* the upgrade succeeded, so the client sees a socket that opens and then closes — which
 * every WebSocket client in existence treats as a transient network fault and retries, turning a bad
 * credential into a reconnect loop.
 *
 * The reference implementation this module replaces got exactly this wrong: it checked for a missing
 * token in a handshake interceptor (good) but hardcoded the parameter name and the failure mode
 * (bad). Here the check is an SPI, and it names the status it wants.
 *
 * Guards are ordinary Spring beans; every one of them runs, in `@Order` sequence, and the first
 * rejection wins. A guard that throws is treated as a rejection — admission control fails closed,
 * because a credential check that blew up must not be read as "credentials fine".
 *
 * ```kotlin
 * @Bean
 * fun tokenPresenceGuard() = IWebSocketHandshakeGuard { request, _ ->
 *     val hasToken = !HandshakeAttrs.parseQuery(request.uri)["token"].isNullOrEmpty()
 *     if (hasToken) WebSocketHandshakeDecision.Proceed
 *     else WebSocketHandshakeDecision.Reject(HttpStatus.UNAUTHORIZED, "missing token")
 * }
 * ```
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface IWebSocketHandshakeGuard {

    /**
     * Decides whether the handshake may proceed.
     *
     * @param request the upgrade request, before any protocol switch
     * @param attributes the handshake attribute map that will become `WebSocketSession.attributes`;
     *   a guard may put values here for the session factory to read, avoiding a second lookup of
     *   whatever it just validated
     */
    fun check(request: ServerHttpRequest, attributes: MutableMap<String, Any>): WebSocketHandshakeDecision
}

/**
 * Outcome of an [IWebSocketHandshakeGuard].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
sealed interface WebSocketHandshakeDecision {

    /** Allow the upgrade; remaining guards still run. */
    data object Proceed : WebSocketHandshakeDecision

    /**
     * Refuse the upgrade. No further guard runs, no WebSocket is established, and the client receives
     * [status] instead of a `101 Switching Protocols`.
     *
     * @param status the HTTP status to answer with; `403` unless the failure is specifically "who are you?"
     * @param reason logged server-side. Deliberately **not** sent to the client: a rejection reason
     *   phrase is exactly the kind of detail ("no such user", "token expired 3 days ago") that helps an
     *   attacker enumerate more than it helps a legitimate client, which only needs the status.
     */
    data class Reject(
        val status: HttpStatus = HttpStatus.FORBIDDEN,
        val reason: String = "",
    ) : WebSocketHandshakeDecision
}
