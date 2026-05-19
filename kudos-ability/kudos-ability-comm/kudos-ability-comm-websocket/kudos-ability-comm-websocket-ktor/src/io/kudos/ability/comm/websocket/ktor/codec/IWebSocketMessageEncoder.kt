package io.kudos.ability.comm.websocket.ktor.codec

/**
 * 业务消息 ↔ WebSocket 文本 frame 的编 / 解码 SPI。
 *
 * 把消息序列化决策从 [io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler]
 * 里解耦出来——同一份 handler 实现可以挂不同的协议（JSON / Protobuf JSON-text / MsgPack-base64
 * 等），业务侧只需要换 encoder。
 *
 * 命名上对称称为 "encoder"，但同时承担 decode——名字简洁优先于纯描述性。
 *
 * 典型实现（在业务侧，不放本模块）：
 *
 * ```kotlin
 * class JacksonWebSocketMessageEncoder(private val mapper: ObjectMapper) : IWebSocketMessageEncoder {
 *     override fun encode(message: Any): String = mapper.writeValueAsString(message)
 *     override fun <T : Any> decode(text: String, type: Class<T>): T = mapper.readValue(text, type)
 * }
 * ```
 *
 * @author K
 * @since 1.0.0
 */
interface IWebSocketMessageEncoder {

    /** 把任意业务对象编码为文本 frame 内容。失败应抛异常——调用方决定是否兜底。 */
    fun encode(message: Any): String

    /** 把收到的文本 frame 内容反向解析为指定类型。 */
    fun <T : Any> decode(text: String, type: Class<T>): T
}
