package io.kudos.ability.comm.websocket.ktor.broadcast

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.kudos.base.logger.LogFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 多端广播 / 单播工具。
 *
 * 依赖 [KudosWebSocketRegistry] 找接收方，然后并发地把消息推送出去。"并发"指各 session
 * 的 `sendText` 用 `async { }` 并行启动；任一 session 的发送失败被 catch + WARN 日志，
 * 不会让其余 session 的发送停掉。
 *
 * **不**做消息批处理 / 速率限制——业务侧需要时自己加。
 *
 * @author K
 * @since 1.0.0
 */
class WebSocketBroadcaster(private val registry: KudosWebSocketRegistry) {

    private val log = LogFactory.getLog(this::class)

    /** 广播给所有连接的 session。 */
    suspend fun broadcast(text: String): Int = sendTo(registry.all(), text)

    /** 广播给指定 `userId` 名下所有 session（多端在线场景）。 */
    suspend fun broadcastToUser(userId: String, text: String): Int =
        sendTo(registry.findByUserId(userId), text)

    /** 广播给指定 `tenantId` 名下所有 session。 */
    suspend fun broadcastToTenant(tenantId: String, text: String): Int =
        sendTo(registry.findByTenantId(tenantId), text)

    /** 单播到指定 sessionId。返回发送是否成功。 */
    suspend fun unicast(sessionId: String, text: String): Boolean {
        val session = registry.findById(sessionId) ?: return false
        return runCatching {
            session.sendText(text)
            true
        }.getOrElse { t ->
            log.warn("单播失败 sessionId={0} cause={1}", sessionId, t.message)
            false
        }
    }

    private suspend fun sendTo(sessions: List<KudosWebSocketSessionRef>, text: String): Int {
        if (sessions.isEmpty()) return 0
        return coroutineScope {
            sessions.map { session ->
                async {
                    runCatching { session.sendText(text); 1 }.getOrElse { t ->
                        log.warn("广播到 sessionId={0} 失败 cause={1}", session.sessionId, t.message)
                        0
                    }
                }
            }.awaitAll().sum()
        }
    }
}
