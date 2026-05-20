package io.kudos.ability.distributed.notify.common.model

import java.io.Serial
import java.io.Serializable

/**
 * 跨节点通知消息载体。
 *
 * @param T messageBody 的具体类型，必须 [Serializable]——会被 MQ 序列化跨进程传递
 * @author Younger
 * @author K
 * @since 1.0.0
 */
class NotifyMessageVo<T : Serializable> : Serializable {
    /**
     * 通知类型——派发依据。默认为空串，让 producer / consumer 可以用 `isBlank()` 做统一防御。
     */
    var notifyType: String = ""

    /**
     * 消息内容
     */
    var messageBody: T? = null

    constructor(messageBody: T) {
        this.messageBody = messageBody
    }

    constructor(notifyType: String, messageBody: T) {
        this.notifyType = notifyType
        this.messageBody = messageBody
    }

    constructor()

    companion object {
        @Serial
        private val serialVersionUID = -1873194994527036525L
    }
}
