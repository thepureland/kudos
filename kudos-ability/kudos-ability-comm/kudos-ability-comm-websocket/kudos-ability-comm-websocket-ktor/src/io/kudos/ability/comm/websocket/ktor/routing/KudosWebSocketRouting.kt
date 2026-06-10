package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.kudos.base.logger.LogFactory
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText

/** Private logger-category anchor: this file only contains top-level functions, so a dedicated object keeps log events attributed to the routing component instead of an unrelated class. */
private object KudosWebSocketRouting

private val log = LogFactory.getLog(KudosWebSocketRouting::class)

/**
 * Ktor `Route` extension that mounts an [IKudosWebSocketHandler] on the WebSocket route at
 * [path]. Handles the full "register → loop → unregister" template:
 *
 * ```kotlin
 * routing {
 *     val registry = ...
 *     kudosWebSocket("/chat", registry) {
 *         it.userId = call.principal<UserPrincipal>()?.id
 *         it.tenantId = call.request.headers["X-Tenant-Id"]
 *         it  // return the final session
 *     } handle ChatHandler()
 * }
 * ```
 *
 * Design notes:
 *  - **`sessionFactory`** lets the business side populate [KudosWebSocketSession] with the
 *    authentication / tenant context from [io.ktor.server.application.ApplicationCall]; the
 *    default implementation returns an anonymous session with no identity.
 *  - **Connection exceptions** are caught and the cause is propagated via
 *    [IKudosWebSocketHandler.onDisconnect].
 *  - **When the route lambda exits**, unregister is always invoked regardless of the cause —
 *    this prevents leaks in the registry.
 *  - **Makes no assumption** about the wire protocol (JSON / Protobuf / etc.) — this extension
 *    only passes frame bytes to the handler; higher-level serialization is performed by the
 *    handler itself (or via an injected business SPI such as `IWebSocketMessageEncoder`).
 *
 * Multiple `kudosWebSocket` routes can share the same [KudosWebSocketRegistry] — the typical
 * pattern is a process-level singleton on the business side.
 *
 * @author K
 * @since 1.0.0
 */
fun Route.kudosWebSocket(
    path: String,
    registry: KudosWebSocketRegistry,
    handler: IKudosWebSocketHandler,
    sessionFactory: (DefaultWebSocketServerSession) -> KudosWebSocketSession = { KudosWebSocketSession(it) },
) {
    webSocket(path) {
        val session = sessionFactory(this)
        registry.register(session)
        var cause: Throwable? = null
        try {
            handler.onConnect(session)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> handler.onText(session, frame.readText())
                    is Frame.Binary -> handler.onBinary(session, frame.readBytes())
                    is Frame.Close -> break
                    else -> {} // Ping / Pong is handled automatically by the Ktor WebSockets plugin; the business side does not need to see it.
                }
            }
        } catch (t: Throwable) {
            cause = t
            log.warn("WebSocket handling exception sessionId={0} path={1} cause={2}",
                session.sessionId, path, t.message)
        } finally {
            runCatching { handler.onDisconnect(session, cause) }
                .onFailure { log.warn("onDisconnect threw an exception sessionId={0} cause={1}", session.sessionId, it.message) }
            registry.unregister(session.sessionId)
            runCatching { close(CloseReason(CloseReason.Codes.NORMAL, "")) }
        }
    }
}
