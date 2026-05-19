package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Kudos 业务层的 WebSocket 会话包装。
 *
 * 在原生 [DefaultWebSocketServerSession] 之上挂业务侧需要的元数据：
 *  - [sessionId]：进程内会话唯一标识（默认 UUID）
 *  - [userId] / [tenantId]：业务侧建立连接时填入；用于按业务维度索引、广播
 *  - [attributes]：自由扩展点（客户端版本、设备 ID、Locale 等）
 *
 * 上层路由代码持有 [KudosWebSocketSession]，而不是直接持有 Ktor 的会话——好处：
 *  - 关闭 / 发送语义统一（[send] / [close]），后续切换底层引擎不必动业务代码
 *  - 多个业务模块拿同一个对象，避免各自维护一份"sessionId → user"映射
 *
 * 线程安全：单个 session 由 Ktor 单 coroutine 处理 frame，发送来自任意 coroutine
 * 都会进入 Ktor 内部的发送通道，本类不再额外加锁。[attributes] 用并发 Map。
 *
 * @author K
 * @since 1.0.0
 */
class KudosWebSocketSession(
    val raw: DefaultWebSocketServerSession,
    override val sessionId: String = UUID.randomUUID().toString(),
    override val userId: String? = null,
    override val tenantId: String? = null,
) : KudosWebSocketSessionRef {
    override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()

    /** 发送一段文本 frame。`raw.send(...)` 的薄封装。 */
    override suspend fun sendText(text: String) {
        raw.send(Frame.Text(text))
    }

    /** 发送一段二进制 frame。 */
    override suspend fun sendBinary(bytes: ByteArray) {
        raw.send(Frame.Binary(true, bytes))
    }

    /**
     * 正常关闭连接。默认 `NORMAL` close reason；业务可显式传 `GOING_AWAY` 等。
     * 关闭后 [raw] 的 incoming channel 会自动 close，对应路由的 lambda 退出。
     */
    override suspend fun close(reason: CloseReason) {
        raw.close(reason)
    }
}
