package io.kudos.ability.distributed.stream.common.model.vo

import java.io.Serializable

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
