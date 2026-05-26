package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Cluster mode configuration.
 */
class RedissonClusterServersProperties {
    /**
     * Load balancer class. Defaults to the round-robin RoundRobinLoadBalancer.
     */
    var loadBalancer: String? = "!<org.redisson.connection.balancer.RoundRobinLoadBalancer> {}"

    /**
     * Minimum number of idle connections to slave nodes.
     */
    var slaveConnectionMinimumIdleSize: Int = 32

    /**
     * Slave node connection pool size.
     */
    var slaveConnectionPoolSize: Int = 64

    /**
     * Minimum number of idle connections to master nodes.
     */
    var masterConnectionMinimumIdleSize: Int = 32

    /**
     * Master node connection pool size.
     */
    var masterConnectionPoolSize: Int = 64

    /**
     * Read only from slave nodes.
     */
    var readMode: String = "SLAVE"

    /**
     * Master node addresses.
     */
    var nodeAddresses: Array<String> = arrayOf("")

    /**
     * Cluster scan interval, in milliseconds.
     */
    var scanInterval: Int = 1000
}
