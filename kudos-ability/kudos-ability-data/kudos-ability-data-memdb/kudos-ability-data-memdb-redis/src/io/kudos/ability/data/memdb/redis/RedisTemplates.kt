package io.kudos.ability.data.memdb.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Multi-data-source [RedisTemplate] container. Multiple Redis instances (master / cache / session, etc.) can be
 * configured per business need. During assembly, [RedisAutoConfiguration] creates a [RedisTemplate] for each
 * `kudos.ability.data.redis.redis-map.<name>` configuration and indexes them in this object by name.
 * [defaultRedisTemplate] is the one referenced by `default-redis`; business code that uses
 * `@Autowired RedisTemplate` will receive this template by default.
 *
 * @property redisTemplateMap name → template instance
 * @property defaultRedisTemplate The default Redis instance.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisTemplates(
    private val redisTemplateMap: MutableMap<String, RedisTemplate<Any, Any?>>,
    var defaultRedisTemplate: RedisTemplate<Any, Any?>,
) {

    /**
     * Looks up the [RedisTemplate] by name; returns null if not found (the caller is responsible for fallback or raising an error).
     */
    fun getRedisTemplate(redisKey: String) = redisTemplateMap[redisKey]

    /** Returns the full internal name → template mapping. */
    fun getRedisTemplateMap() = redisTemplateMap

    companion object {
        /** Default key serializer (UTF-8 string); recommended for hash field names too. */
        val REDIS_KEY_SERIALIZER: StringRedisSerializer = StringRedisSerializer.UTF_8
    }
}
