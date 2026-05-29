package io.kudos.ability.comm.websocket.ktor.distributed

import java.io.Serializable

/**
 * Wire payload that crosses process boundaries through an [IWebSocketBroadcastChannel].
 *
 * Carries:
 * - [nodeId]: originating node identity — receivers compare against their own to filter self-echoes,
 *   so a single instance does not deliver its own broadcast twice (once locally + once from pub/sub).
 * - [targetType] + [targetId]: which set of local sessions on the receiving node should be fanned to.
 *   Encoded as a flat enum + nullable id rather than a sealed hierarchy so that JSON/Jackson and Spring's
 *   default `RedisTemplate` value serializer can handle it without polymorphic-type wiring.
 * - [text]: the actual text frame payload.
 *
 * Binary frames are not modeled yet — the first port keeps the surface narrow; if needed, add a sibling
 * `WebSocketBinaryBroadcastEnvelope` rather than overloading [text] with base64.
 *
 * Java serialization is enabled (the default Spring `JdkSerializationRedisSerializer` path).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class WebSocketBroadcastEnvelope(
    val nodeId: String,
    val targetType: TargetType,
    val targetId: String?,
    val text: String,
) : Serializable {

    /** Fan-out target encoding. Flat enum so the envelope serializes cleanly through any default codec. */
    enum class TargetType {
        /** Every locally-registered session. */
        ALL,
        /** All sessions whose `userId` matches [WebSocketBroadcastEnvelope.targetId]. */
        USER,
        /** All sessions whose `tenantId` matches [WebSocketBroadcastEnvelope.targetId]. */
        TENANT,
        /** The single session whose `sessionId` matches [WebSocketBroadcastEnvelope.targetId]. */
        SESSION,
    }

    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 1L
    }
}
