package io.kudos.ability.distributed.lock.redisson.init.properties

class RedissonProperties {
    var enabled: Boolean = false

    var mode: String? = "single"

    var config: RedissonConfigProperties? = null

    var baseConfig: RedissonBaseConfigProperties? = null

    var singleServerConfig: RedissonSingleServerProperties? = null

    var clusterServersConfig: RedissonClusterServersProperties? = null
}
