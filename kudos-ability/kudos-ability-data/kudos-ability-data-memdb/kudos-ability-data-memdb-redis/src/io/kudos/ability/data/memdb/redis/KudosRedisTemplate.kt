package io.kudos.ability.data.memdb.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

class KudosRedisTemplate {

    /**
     * 多个redisTemplate
     */
    private var redisTemplateMap: MutableMap<String, RedisTemplate<Any, Any?>>

    /**
     * default 的 redisTemplate
     */
    var defaultRedisTemplate: RedisTemplate<Any, Any?>

    constructor(
        redisTemplateMap: MutableMap<String, RedisTemplate<Any, Any?>>,
        defaultRedisTemplate: RedisTemplate<Any, Any?>
    ) {
        this.redisTemplateMap = redisTemplateMap
        this.defaultRedisTemplate = defaultRedisTemplate
    }

    fun getRedisTemplate(redisKey: String) = redisTemplateMap[redisKey]

    fun getRedisTemplateMap() = redisTemplateMap


    companion object Companion {
        val REDIS_KEY_SERIALIZER: StringRedisSerializer = StringRedisSerializer.UTF_8
    }

}
