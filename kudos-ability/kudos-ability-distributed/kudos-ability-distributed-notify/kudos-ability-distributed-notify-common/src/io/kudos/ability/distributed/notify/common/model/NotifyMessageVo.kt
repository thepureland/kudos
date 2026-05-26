package io.kudos.ability.distributed.notify.common.model

import java.io.Serial
import java.io.Serializable

/**
 * Payload for cross-node notification messages.
 *
 * @param T concrete type of messageBody; must be [Serializable]—it will be serialized by MQ and transmitted across processes
 * @author Younger
 * @author K
 * @since 1.0.0
 */
class NotifyMessageVo<T : Serializable> : Serializable {
    /**
     * Notification type—the dispatch key. Defaults to an empty string so both producer and consumer can apply
     * a uniform `isBlank()` defensive check.
     */
    var notifyType: String = ""

    /**
     * Message content.
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
