package io.kudos.ability.data.memdb.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * 多数据源 RedisTemplate 容器；[defaultRedisTemplate] 为默认实例。
 */
class RedisTemplates(
    private val redisTemplateMap: MutableMap<String, RedisTemplate<Any, Any?>>,
    var defaultRedisTemplate: RedisTemplate<Any, Any?>,
) {

    fun getRedisTemplate(redisKey: String) = redisTemplateMap[redisKey]

    fun getRedisTemplateMap() = redisTemplateMap

    companion object {
        val REDIS_KEY_SERIALIZER: StringRedisSerializer = StringRedisSerializer.UTF_8
    }
}
