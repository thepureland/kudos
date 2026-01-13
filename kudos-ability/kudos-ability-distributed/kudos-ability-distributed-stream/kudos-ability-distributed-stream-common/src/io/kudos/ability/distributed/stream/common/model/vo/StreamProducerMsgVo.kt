package io.kudos.ability.distributed.stream.common.model.vo

import java.io.Serializable

/**
 * 流式消息生产者消息值对象
 * 用于封装发送失败的消息信息，包括绑定名称、消息头和消息体
 */
class StreamProducerMsgVo : Serializable {
    /**
     * 消息主题
     */
    var bindName: String? = null

    /**
     * 消息头json串
     */
    var msgHeaderJson: String? = null

    /**
     * 消息体json串
     */
    var msgBodyJson: String? = null
}
