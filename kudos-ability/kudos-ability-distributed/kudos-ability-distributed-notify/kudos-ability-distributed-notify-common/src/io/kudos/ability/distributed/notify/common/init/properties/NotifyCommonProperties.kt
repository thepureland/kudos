package io.kudos.ability.distributed.notify.common.init.properties

/**
 * Common notify configuration.
 *
 * Binding prefix: `kudos.ability.distributed.notify`.
 */
open class NotifyCommonProperties {

    /**
     * When true, if no [io.kudos.ability.distributed.notify.common.api.INotifyProducer] is wired,
     * fail fast at startup.
     */
    var failOnMissingProducer: Boolean = false

    /**
     * Listener namespace. When blank, defaults to `spring.application.name`.
     */
    var listenerNamespace: String? = null

    /**
     * Whether to continue falling back to the default namespace when no listener is matched in the current namespace.
     *
     * Disabled by default to avoid misrouting messages to the default listener when multiple applications share the
     * same MQ topic and register the same notifyType.
     */
    var fallbackToDefaultNamespace: Boolean = false
}
