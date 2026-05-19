package io.kudos.ability.comm.websocket.ktor.handler

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession

/**
 * 业务侧 WebSocket 处理 SPI。
 *
 * 实现这个接口的类负责：
 *  - 接受新连接（[onConnect]）——通常用于鉴权、装上下文、推欢迎消息
 *  - 处理每一帧文本 / 二进制（[onText] / [onBinary]）
 *  - 收尾（[onDisconnect]）——清理业务侧持有的资源
 *
 * 实现类**不需要**显式管理 [KudosWebSocketSession] 在 [io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry]
 * 的注册——上层路由扩展函数（参 `kudosWebSocket` 的实现）已经按"register → handle → unregister"
 * 模板调度本接口。
 *
 * 接口默认实现都是 no-op，业务侧按需 override。
 *
 * @author K
 * @since 1.0.0
 */
interface IKudosWebSocketHandler {

    /** 连接建立后、首条业务 frame 之前被调用。 */
    suspend fun onConnect(session: KudosWebSocketSession) {}

    /** 收到一段文本 frame。`text` 已是 UTF-8 解码后的内容。 */
    suspend fun onText(session: KudosWebSocketSession, text: String) {}

    /** 收到一段二进制 frame。 */
    suspend fun onBinary(session: KudosWebSocketSession, bytes: ByteArray) {}

    /**
     * 连接关闭（正常 / 异常）。`cause` 非 null 表示是异常断开——业务侧可据此决定是否清理
     * 持久化的"在线状态"标记。本方法**之前** registry 还持有 session；
     * 之后会自动 unregister，所以这里仍可调 `session.sendXxx` 但很可能写入失败
     * （连接已不可达），业务侧别依赖。
     */
    suspend fun onDisconnect(session: KudosWebSocketSession, cause: Throwable? = null) {}
}
