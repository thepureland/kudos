package io.kudos.ability.distributed.notify.mq.init.properties

/**
 * notify-mq extension configuration.
 *
 * Binding prefix: `kudos.ability.distributed.notify.mq`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class NotifyMqProperties {

    /**
     * When true, startup fails immediately if no producer binding configuration is found.
     */
    var failOnMissingProducerBinding: Boolean = false

    /**
     * Whether to rethrow exceptions on consumer-side failures.
     *
     * Default false preserves historical behavior: log and ack only. Recommended to enable in
     * production so the underlying MQ binder can retry per config or route to a DLQ.
     */
    var rethrowConsumerException: Boolean = false
}
