package io.kudos.ability.data.memdb.redis.init.properties

class SoulRedisProperties {
    var defaultRedis: String? = null
    var redisMap: MutableMap<String, RedisExtProperties> = HashMap()
}
