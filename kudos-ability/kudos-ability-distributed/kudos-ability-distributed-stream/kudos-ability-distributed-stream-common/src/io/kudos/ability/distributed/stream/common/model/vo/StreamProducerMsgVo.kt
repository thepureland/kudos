package io.kudos.ability.distributed.stream.common.model.vo

import java.io.Serializable

/**
 * Stream producer message value object.
 * Wraps failed message info, including the binding name, message header and message body.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class StreamProducerMsgVo : Serializable {
    /**
     * Message topic.
     */
    var bindName: String? = null

    /**
     * Message header JSON string.
     */
    var msgHeaderJson: String? = null

    /**
     * Message body JSON string.
     */
    var msgBodyJson: String? = null

    /**
     * Runtime type name of the message body.
     *
     * On retry, deserialization is attempted with this type first; when null or recovery
     * fails, it falls back to a dynamic JSON structure such as Map / List.
     */
    var msgBodyClassName: String? = null
}
