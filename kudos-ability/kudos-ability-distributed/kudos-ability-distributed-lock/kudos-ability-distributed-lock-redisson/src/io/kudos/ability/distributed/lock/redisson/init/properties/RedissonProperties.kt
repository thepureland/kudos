package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Redisson config properties.
 * Holds the full Redisson configuration: enable flag, run mode and server settings.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedissonProperties {
    var enabled: Boolean = false

    var mode: String = "single"

    /**
     * Uniform prefix appended by RedissonLockKit when building Redis lock keys. Set to empty string to disable.
     */
    var lockKeyPrefix: String = "REDISSON::"

    var config: RedissonConfigProperties? = null

    var baseConfig: RedissonBaseConfigProperties? = null

    var singleServerConfig: RedissonSingleServerProperties? = null

    var clusterServersConfig: RedissonClusterServersProperties? = null
}
