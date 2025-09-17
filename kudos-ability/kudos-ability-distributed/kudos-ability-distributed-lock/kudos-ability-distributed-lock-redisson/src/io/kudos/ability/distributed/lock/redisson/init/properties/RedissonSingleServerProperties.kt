package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * 单机模式配置
 */
class RedissonSingleServerProperties {
    /**
     * 节点地址
     */
    var address: String? = null

    /**
     * 最小空闲连接数
     */
    var connectionMinimumIdleSize: Int = 32

    /**
     * 连接池大小
     */
    var connectionPoolSize: Int = 64

    /**
     * 数据库编号
     */
    var database: Int = 0
}
