package io.kudos.ability.comm.websocket.common.distributed

import io.kudos.ability.comm.websocket.common.session.WebSocketPayload
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
 * - [text] **or** [binary]: the message itself; exactly one is non-null. Two flat nullable fields for
 *   the same reason the target is flat — a sealed `payload` property would need polymorphic type
 *   information in every codec. [payload] reads them back as a [WebSocketPayload].
 * - [version]: envelope schema version, so a rolling upgrade can add fields without the old nodes
 *   mis-reading new traffic. Receivers drop anything newer than they understand ([CURRENT_VERSION]).
 *
 * Java serialization is enabled (the default Spring `JdkSerializationRedisSerializer` path).
 * `equals` / `hashCode` are hand-written because the generated ones would compare [binary] by
 * reference.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class WebSocketBroadcastEnvelope(
    val nodeId: String,
    val targetType: TargetType,
    val targetId: String?,
    val text: String? = null,
    val binary: ByteArray? = null,
    val version: Int = CURRENT_VERSION,
) : Serializable {

    init {
        require((text == null) != (binary == null)) {
            "WebSocketBroadcastEnvelope must carry exactly one of text / binary"
        }
    }

    /** The message as a [WebSocketPayload], or `null` if a malformed envelope carried neither field. */
    val payload: WebSocketPayload?
        get() = when {
            text != null -> WebSocketPayload.Text(text)
            binary != null -> WebSocketPayload.Binary(binary)
            else -> null
        }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSocketBroadcastEnvelope) return false
        return nodeId == other.nodeId &&
            targetType == other.targetType &&
            targetId == other.targetId &&
            text == other.text &&
            binary.contentEquals(other.binary) &&
            version == other.version
    }

    override fun hashCode(): Int {
        var result = nodeId.hashCode()
        result = 31 * result + targetType.hashCode()
        result = 31 * result + (targetId?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (binary?.contentHashCode() ?: 0)
        result = 31 * result + version
        return result
    }

    /** Prints the binary size rather than its content — payloads can be megabytes. */
    override fun toString(): String =
        "WebSocketBroadcastEnvelope(nodeId=$nodeId, targetType=$targetType, targetId=$targetId, " +
            "text=$text, binary=${binary?.let { "${it.size} bytes" }}, version=$version)"

    companion object {
        /** Schema version written by this build. Bump when adding a field that older nodes must not silently ignore. */
        const val CURRENT_VERSION: Int = 1

        /** Builds an envelope from a [WebSocketPayload], picking the right flat field. */
        @JvmStatic
        fun of(
            nodeId: String,
            targetType: TargetType,
            targetId: String?,
            payload: WebSocketPayload,
        ): WebSocketBroadcastEnvelope = when (payload) {
            is WebSocketPayload.Text -> WebSocketBroadcastEnvelope(nodeId, targetType, targetId, text = payload.text)
            is WebSocketPayload.Binary -> WebSocketBroadcastEnvelope(nodeId, targetType, targetId, binary = payload.bytes)
        }

        @JvmStatic
        private val serialVersionUID: Long = 1L
    }
}
