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
     * 通知类型——派发依据。Java 序列化跨进程时如果 sender 没用三参构造器赋值，此处
     * 会 deserialize 出 `lateinit not initialized` 状态；调用方读取前应自行判断。
     */
    lateinit var notifyType: String

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
