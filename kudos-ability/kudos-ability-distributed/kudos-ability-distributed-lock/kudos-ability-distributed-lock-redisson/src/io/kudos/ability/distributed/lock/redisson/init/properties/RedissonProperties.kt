package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Redisson配置属性类
 * 封装Redisson的完整配置信息，包括启用开关、运行模式、服务器配置等
 */
class RedissonProperties {
    var enabled: Boolean = false

    var mode: String? = "single"

    var config: RedissonConfigProperties? = null

    var baseConfig: RedissonBaseConfigProperties? = null

    var singleServerConfig: RedissonSingleServerProperties? = null

    var clusterServersConfig: RedissonClusterServersProperties? = null
}
