package io.kudos.ability.distributed.notify.common.api

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable


/**
 * SPI for the producer of cross-node notification messages.
 *
 * Triggered indirectly by callers via `NotifyTool.notify(messageVo)`; concrete delivery implementations live
 * in modules such as `notify-mq` (bean name [BEAN_NAME]). Common use cases: cache invalidation broadcasts,
 * config change notifications, multi-node state synchronization.
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
interface INotifyProducer {

    /**
     * Sends a message to cluster nodes. Returns true when the send succeeds; exact semantics depend on the
     * implementation (at-least-once / fire-and-forget).
     *
     * @param messageVo the notification message payload (contains notifyType + messageBody)
     */
    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean

    companion object {
        /** Convention for the Spring bean name; resolve implementations by this name when multiple exist. */
        const val BEAN_NAME: String = "notifyMqProducer"
    }

}
