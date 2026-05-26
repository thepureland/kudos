package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Redisson base config properties.
 * Encapsulates the base Redisson client connection settings: connect timeout, command timeout, retry attempts, etc.
 */
class RedissonBaseConfigProperties {
    /**
     * Ping command interval. Set to 0 to disable. Default is 0.
     */
    var pingConnectionInterval: Int = 0

    /**
     * Idle connection timeout. If the connection pool size exceeds the minimum idle size and a connection has been idle
     * longer than this value, it is closed and removed from the pool. In milliseconds.
     */
    var idleConnectionTimeout: Int = 10000

    /**
     * Connect timeout, in milliseconds.
     */
    var connectTimeout: Int = 10000

    /**
     * Command wait timeout, in milliseconds.
     */
    var timeout: Int = 3000

    /**
     * Command retry count. If retryAttempts is reached without successfully sending the command to a node, an error is
     * thrown. If sending succeeds within the limit, the timeout (command wait timeout) timer starts.
     */
    var retryAttempts: Int = 3

    /**
     * Command retry interval, in milliseconds.
     */
    var retryInterval: Int = 1500

    /**
     * Password.
     */
    var password: String? = null

    /**
     * Maximum subscriptions per connection.
     */
    var subscriptionsPerConnection: Int = 5

    /**
     * Client name.
     */
    var clientName: String? = ""

    /**
     * Minimum idle connections for the pub/sub pool.
     */
    var subscriptionConnectionMinimumIdleSize: Int = 1

    /**
     * Pub/sub connection pool size.
     */
    var subscriptionConnectionPoolSize: Int = 50

    /**
     * DNS monitoring interval, in milliseconds.
     */
    var dnsMonitoringInterval: Long = 5000L
}
