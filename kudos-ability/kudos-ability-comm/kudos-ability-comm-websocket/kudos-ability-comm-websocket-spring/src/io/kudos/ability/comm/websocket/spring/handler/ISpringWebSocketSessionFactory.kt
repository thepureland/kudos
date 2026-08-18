package io.kudos.ability.comm.websocket.spring.handler

import io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession
import org.springframework.web.socket.WebSocketSession

/**
 * Identification seam: turns an established Spring [WebSocketSession] into a
 * [SpringWebSocketSession] carrying `userId` / `tenantId`, or refuses the connection by returning
 * `null`.
 *
 * The Spring counterpart of the `sessionFactory` lambda on Ktor's `Route.kudosWebSocket`, and it
 * runs at the same point in the lifecycle: after the upgrade, before the session enters the
 * registry. Rejecting *here* rather than later inside `onConnect` matters — the session is
 * registered immediately afterwards, so a connection admitted first and closed later is, for that
 * window, a member of its claimed `tenantId` and receives that tenant's broadcasts.
 *
 * The method is `suspend`, so validating a token against a database or Redis is a plain call rather
 * than something that has to block a container thread. It runs on the session's own pump coroutine,
 * so a slow factory delays only that connection.
 *
 * Everything the handshake knew is reachable through `session.attributes`, which
 * [io.kudos.ability.comm.websocket.spring.handshake.KudosHandshakeInterceptor] populates under the
 * keys in [io.kudos.ability.comm.websocket.spring.handshake.HandshakeAttrs]:
 *
 * ```kotlin
 * @Bean
 * fun wsSessionFactory(userService: UserService) = ISpringWebSocketSessionFactory { raw ->
 *     val token = HandshakeAttrs.queryParam(raw, "token") ?: return@ISpringWebSocketSessionFactory null
 *     val user = userService.findByToken(token) ?: return@ISpringWebSocketSessionFactory null
 *     SpringWebSocketSession(raw, userId = user.id, tenantId = user.tenantId)
 * }
 * ```
 *
 * ## Which seam to use
 *
 * Three hooks sit on the connection path, and they are not interchangeable:
 *
 *  - [io.kudos.ability.comm.websocket.spring.handshake.IWebSocketHandshakeGuard] runs **before** the
 *    upgrade, so it can answer with a real HTTP status (401 / 403). Use it for anything a client
 *    should learn about as an HTTP error rather than as a WebSocket that opens and immediately shuts.
 *  - This factory establishes **identity**. Returning `null` means "cannot tell who this is".
 *  - [io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor] applies
 *    **policy** to a connection whose identity is already known (per-user device caps, per-tenant
 *    quotas), composes as a list, and is shared with the Ktor module.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface ISpringWebSocketSessionFactory {

    /**
     * Builds the business session for [session], or returns `null` to reject the connection.
     *
     * @param session the established session, already wrapped for concurrent sends
     */
    suspend fun create(session: WebSocketSession): SpringWebSocketSession?
}
