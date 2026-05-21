package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Redisson配置属性类
 * 封装Redisson的完整配置信息，包括启用开关、运行模式、服务器配置等
 */
class RedissonProperties {
    var enabled: Boolean = false

    var mode: String = "single"

    /**
     * RedissonLockKit 生成 Redis lock key 时追加的统一前缀。可设为空字符串禁用前缀。
     */
    var lockKeyPrefix: String = "REDISSON::"

    var config: RedissonConfigProperties? = null

    var baseConfig: RedissonBaseConfigProperties? = null

    var singleServerConfig: RedissonSingleServerProperties? = null

    var clusterServersConfig: RedissonClusterServersProperties? = null
}
