package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * 集群模式配置
 */
class RedissonClusterServersProperties {
    /**
     * 负载均衡算法类的选择  默认轮询调度算法RoundRobinLoadBalancer
     */
    var loadBalancer: String? = "!<org.redisson.connection.balancer.RoundRobinLoadBalancer> {}"

    /**
     * 从节点最小空闲连接数
     */
    var slaveConnectionMinimumIdleSize: Int = 32

    /**
     * 从节点连接池大小
     */
    var slaveConnectionPoolSize: Int = 64

    /**
     * 主节点最小空闲连接数
     */
    var masterConnectionMinimumIdleSize: Int = 32

    /**
     * 主节点连接池大小
     */
    var masterConnectionPoolSize: Int = 64

    /**
     * 只在从服务节点里读取
     */
    var readMode: String = "SLAVE"

    /**
     * 主节点信息
     */
    var nodeAddresses: Array<String> = arrayOf<String>("")

    /**
     * 集群扫描间隔时间 单位毫秒
     */
    var scanInterval: Int = 1000
}
