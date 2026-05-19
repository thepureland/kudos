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

private val log = LogFactory.getLog(KudosWebSocketRegistry::class)

/**
 * Ktor `Route` 扩展函数：把一个 [IKudosWebSocketHandler] 挂到 [path] 上的 WebSocket
 * 路由。负责完整的"register → loop → unregister"模板：
 *
 * ```kotlin
 * routing {
 *     val registry = ...
 *     kudosWebSocket("/chat", registry) {
 *         it.userId = call.principal<UserPrincipal>()?.id
 *         it.tenantId = call.request.headers["X-Tenant-Id"]
 *         it  // 返回最终的 session
 *     } handle ChatHandler()
 * }
 * ```
 *
 * 设计要点：
 *  - **`sessionFactory`** 让业务侧把 [io.ktor.server.application.ApplicationCall] 里的认证 /
 *    租户上下文回填到 [KudosWebSocketSession]；默认实现返回不带身份的匿名 session
 *  - **连接异常**会被 catch + 通过 [IKudosWebSocketHandler.onDisconnect] 透传 cause
 *  - **路由 lambda 退出** 时无论原因都会 unregister——防止注册中心泄漏
 *  - **不假定**业务侧用 JSON / Protobuf 等具体协议——本扩展只把 frame 字节传给 handler，
 *    更高层的序列化由 handler 自己做（或注入 `IWebSocketMessageEncoder` 等业务 SPI）
 *
 * 多个 `kudosWebSocket` 路由可以共享同一个 [KudosWebSocketRegistry]——业务侧典型做法是
 * 进程级单例。
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
                    else -> {} // Ping / Pong 由 Ktor WebSockets 插件自动应答，业务侧不需要看
                }
            }
        } catch (t: Throwable) {
            cause = t
            log.warn("WebSocket 处理异常 sessionId={0} path={1} cause={2}",
                session.sessionId, path, t.message)
        } finally {
            runCatching { handler.onDisconnect(session, cause) }
                .onFailure { log.warn("onDisconnect 抛异常 sessionId={0} cause={1}", session.sessionId, it.message) }
            registry.unregister(session.sessionId)
            runCatching { close(CloseReason(CloseReason.Codes.NORMAL, "")) }
        }
    }
}
