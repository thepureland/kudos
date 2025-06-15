package io.kudos.ability.distributed.notify.common.model

import java.io.Serial
import java.io.Serializable

/**
 * 创建人： Younger
 * 日期： 2022/11/14 12:08
 * 描述：
 */
class NotifyMessageVo<T : Serializable> : Serializable {
    /**
     * 通知类型
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
