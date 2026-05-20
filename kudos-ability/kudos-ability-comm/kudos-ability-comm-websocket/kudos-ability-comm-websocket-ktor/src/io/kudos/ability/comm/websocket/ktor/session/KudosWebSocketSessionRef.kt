package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.websocket.CloseReason

/**
 * 注册中心 / 广播器使用的会话**抽象**。
 *
 * 抽出来的目的：让 [KudosWebSocketRegistry] /
 * [io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster] 等业务层组件不直接
 * 依赖 Ktor 的 `DefaultWebSocketServerSession`——
 *
 *  - **测试性**：单测里可以用纯数据对象实现本接口，无需启动 Ktor / 模拟 WebSocket 上下文
 *  - **多引擎兼容**：未来若新增 netty 直连等"非 Ktor"实现，复用同一套注册 / 广播抽象只需要
 *    新的 [KudosWebSocketSessionRef] 实现
 *
 * 真正运行时还是用 [KudosWebSocketSession]——它同时实现本接口且持有 `raw` Ktor 会话。
 *
 * @author K
 * @since 1.0.0
 */
interface KudosWebSocketSessionRef {
    /** 进程内唯一标识。 */
    val sessionId: String

    /** 业务侧建立连接时填入；为 null 表示匿名会话。 */
    val userId: String?

    /** 业务侧建立连接时填入；多租户场景的隔离键。 */
    val tenantId: String?

    /** 自由扩展点（客户端版本、设备 ID、Locale 等）。 */
    val attributes: MutableMap<String, Any?>

    /** 发送一段文本 frame。 */
    suspend fun sendText(text: String)

    /** 发送一段二进制 frame。 */
    suspend fun sendBinary(bytes: ByteArray)

    /** 正常关闭连接。 */
    suspend fun close(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""))
}
