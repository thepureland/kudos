package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Single-server mode configuration.
 */
class RedissonSingleServerProperties {
    /**
     * Node address.
     */
    var address: String? = null

    /**
     * Minimum number of idle connections.
     */
    var connectionMinimumIdleSize: Int = 32

    /**
     * Connection pool size.
     */
    var connectionPoolSize: Int = 64

    /**
     * Database index.
     */
    var database: Int = 0
}
