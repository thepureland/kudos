package io.kudos.ability.data.memdb.redis.init.properties

/**
 * Aggregate configuration container for Redis; corresponds to `kudos.ability.data.redis.*`.
 *
 * @property defaultRedis name of the default redis instance to use (must exist in [redisMap])
 * @property redisMap name -> per-instance configuration; business code retrieves the corresponding RedisTemplate by name
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisProperties {
    var defaultRedis: String? = null
    var redisMap = mutableMapOf<String, RedisExtProperties>()
}
