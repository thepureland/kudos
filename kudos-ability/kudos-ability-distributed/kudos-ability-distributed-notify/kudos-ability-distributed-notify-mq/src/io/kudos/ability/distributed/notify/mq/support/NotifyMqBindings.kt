package io.kudos.ability.distributed.notify.mq.support

/**
 * Default binding name constants for notify-mq.
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
