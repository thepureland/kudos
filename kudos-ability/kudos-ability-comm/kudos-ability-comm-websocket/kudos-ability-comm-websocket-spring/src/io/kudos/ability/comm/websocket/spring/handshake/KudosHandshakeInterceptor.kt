package io.kudos.ability.comm.websocket.spring.handshake

import io.kudos.base.logger.LogFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * The handshake interceptor kudos mounts on every endpoint it registers.
 *
 * Does two things, and deliberately nothing else:
 *
 *  1. **Parses the query string once** and caches it under [HandshakeAttrs.QUERY_PARAMS], so the
 *     session factory and any guard read a `Map` instead of each re-parsing `uri.rawQuery`.
 *  2. **Runs the [IWebSocketHandshakeGuard]s**, denying the upgrade with a real HTTP status when one
 *     rejects.
 *
 * It does *not* copy headers, the principal or the remote address into the attribute map the way
 * hand-rolled interceptors usually do — [org.springframework.web.socket.WebSocketSession] already
 * exposes all three, and a copy would just be a staler second source.
 *
 * It also does not extend `HttpSessionHandshakeInterceptor`. That superclass copies HTTP session
 * attributes into the WebSocket session and, by default, *creates* an `HttpSession` if none exists —
 * which quietly makes every WebSocket connection allocate server-side session state even for a
 * token-authenticated client that has no session and wants none.
 *
 * ## Failing closed
 *
 * A guard that throws is logged and treated as a rejection. The alternative — letting the exception
 * propagate — ends in the container's generic 500 handling, which is both a worse client experience
 * and, worse, easy to mistake for a transient error and retry. A quota or credential check that blew
 * up must never be read as "allowed".
 *
 * @param guards admission checks run in order; the first rejection wins. Empty means "allow every
 *   upgrade", which is the right default for an endpoint whose authentication is handled by Spring
 *   Security ahead of it.
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class KudosHandshakeInterceptor(
    private val guards: List<IWebSocketHandshakeGuard> = emptyList(),
) : HandshakeInterceptor {

    private val log = LogFactory.getLog(this::class)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        attributes[HandshakeAttrs.QUERY_PARAMS] = HandshakeAttrs.parseQuery(request.uri)

        for (guard in guards) {
            val decision = try {
                guard.check(request, attributes)
            } catch (t: Throwable) {
                log.warn("WebSocket handshake guard {0} threw, rejecting upgrade uri={1} cause={2}",
                    guard::class.simpleName, request.uri, t.message)
                WebSocketHandshakeDecision.Reject(reason = "guard failed")
            }
            if (decision is WebSocketHandshakeDecision.Reject) {
                log.warn("WebSocket handshake rejected by {0} uri={1} status={2} reason={3}",
                    guard::class.simpleName, request.uri, decision.status.value(), decision.reason)
                response.setStatusCode(decision.status)
                return false
            }
        }
        return true
    }

    /**
     * No-op. The upgrade either happened or did not; [exception] is already logged by Spring's
     * `HandshakeHandler`, and the business side gets a far more useful signal from
     * `IKudosWebSocketHandler.onConnect` / `onDisconnect`.
     */
    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // intentionally empty
    }
}
