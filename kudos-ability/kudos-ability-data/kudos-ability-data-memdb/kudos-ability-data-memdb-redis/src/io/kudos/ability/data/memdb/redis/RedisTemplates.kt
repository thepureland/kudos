package io.kudos.ability.data.memdb.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * 多数据源 [RedisTemplate] 容器。按业务可以配多个 redis 实例（master / cache / session 等），
 * 由 [RedisAutoConfiguration] 在装配时根据 `kudos.ability.data.redis.redis-map.<name>` 配置一份
 * [RedisTemplate]，并按 name 索引到本对象。[defaultRedisTemplate] 是 `default-redis` 指向的
 * 那一份，业务代码 `@Autowired RedisTemplate` 时默认拿这个。
 *
 * @property redisTemplateMap name → 模板实例
 * @property defaultRedisTemplate 默认 redis 实例
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisTemplates(
    private val redisTemplateMap: MutableMap<String, RedisTemplate<Any, Any?>>,
    var defaultRedisTemplate: RedisTemplate<Any, Any?>,
) {

    /**
     * 按 name 查指定的 [RedisTemplate]，找不到返回 null（业务侧负责回退或抛错）。
     */
    fun getRedisTemplate(redisKey: String) = redisTemplateMap[redisKey]

    /** 返回内部 name → 模板的全量映射。 */
    fun getRedisTemplateMap() = redisTemplateMap

    companion object {
        /** key 默认序列化器（UTF-8 字符串）；hash 字段名也建议复用。 */
        val REDIS_KEY_SERIALIZER: StringRedisSerializer = StringRedisSerializer.UTF_8
    }
}
