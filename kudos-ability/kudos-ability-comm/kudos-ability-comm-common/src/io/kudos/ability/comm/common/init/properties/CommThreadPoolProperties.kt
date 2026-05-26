package io.kudos.ability.comm.common.init.properties

/**
 * Shared thread pool configuration for communication modules (email / SMS / push / WebSocket).
 *
 * **Note: currently** no specific module loads this configuration (comm-email / comm-sms-* all
 * use virtual threads independently). It is kept for future scenarios that require a "shared
 * synchronous send thread pool". If it remains unused, consider evaluating this module for removal.
 *
 * @author K
 * @since 1.0.0
 */
class CommThreadPoolProperties {
    /**
     * Thread name prefix, default is comm-pool.
     */
    var threadNamePrefix: String = "comm-pool"

    /**
     * Minimum number of threads maintained by the pool, default is 3.
     */
    var corePoolSize: Int = 3

    /**
     * Maximum number of threads maintained by the pool, default is 10.
     */
    var maxPoolSize: Int = 10

    /**
     * Idle time allowed for threads maintained by the pool, default is 900 seconds.
     */
    var keepAliveSeconds: Int = 900

    /**
     * Buffer queue used by the thread pool, default is 100.
     */
    var queueCapacity: Int = 100
}
