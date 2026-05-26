package io.kudos.ability.comm.websocket.ktor.codec

/**
 * SPI for encoding / decoding business messages to and from WebSocket text frames.
 *
 * Decouples the message serialization decision from
 * [io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler] — the same handler
 * implementation can be wired with different protocols (JSON / Protobuf JSON-text /
 * MsgPack-base64 etc.); the business side only needs to swap the encoder.
 *
 * The name is "encoder" for symmetry, but it also handles decoding — brevity is preferred over
 * pure descriptiveness.
 *
 * Typical implementation (lives on the business side, not in this module):
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

    /** Encodes an arbitrary business object as text frame content. Failures should throw an exception — the caller decides on a fallback. */
    fun encode(message: Any): String

    /** Decodes received text frame content into the given type. */
    fun <T : Any> decode(text: String, type: Class<T>): T
}
