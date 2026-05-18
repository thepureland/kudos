package io.kudos.ability.data.memdb.redis.init.properties

/**
 * Redis 总配置容器；对应 `kudos.ability.data.redis.*`。
 *
 * @property defaultRedis 默认使用的 redis 实例 name（必须存在于 [redisMap]）
 * @property redisMap name → 单实例配置，业务侧通过 name 索引拿对应 RedisTemplate
 * @author K
 * @since 1.0.0
 */
class RedisProperties {
    var defaultRedis: String? = null
    var redisMap = mutableMapOf<String, RedisExtProperties>()
}
