package io.kudos.ability.distributed.notify.mq.support

/**
 * notify-mq 默认 binding 名称常量。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object NotifyMqBindings {
    const val TOPIC = "mqNotify"
    const val PRODUCER_BINDING = "mqNotify-out-0"
    const val CONSUMER_BINDING = "mqNotify-in-0"
    const val CONSUMER_BEAN = "mqNotify"
}
