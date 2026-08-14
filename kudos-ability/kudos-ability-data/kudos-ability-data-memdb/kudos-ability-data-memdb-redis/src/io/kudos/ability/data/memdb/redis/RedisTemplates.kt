package io.kudos.ability.data.memdb.redis

import org.springframework.beans.factory.DisposableBean
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Multi-data-source [RedisTemplate] container. Multiple Redis instances (master / cache / session, etc.) can be
 * configured per business need. During assembly, [RedisAutoConfiguration] creates a [RedisTemplate] for each
 * `kudos.ability.data.redis.redis-map.<name>` configuration and indexes them in this object by name.
 * [defaultRedisTemplate] is the one referenced by `default-redis`; business code that uses
 * `@Autowired RedisTemplate` will receive this template by default.
 *
 * The class is `open` so containers (lazy-injection proxies, mocking frameworks) can subclass it;
 * all lookups are read-only and the internal maps are defensively copied.
 *
 * As the only bean holding the per-instance [LettuceConnectionFactory]s, this class implements
 * [DisposableBean] and closes every factory (connection pools + Lettuce client threads) when the
 * application context shuts down.
 *
 * @property defaultRedisTemplate The default Redis instance.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class RedisTemplates(
    redisTemplateMap: Map<String, RedisTemplate<Any, Any?>>,
    open val defaultRedisTemplate: RedisTemplate<Any, Any?>,
    connectionFactoryMap: Map<String, LettuceConnectionFactory> = emptyMap(),
) : DisposableBean {

    private val redisTemplateMap: Map<String, RedisTemplate<Any, Any?>> = redisTemplateMap.toMap()

    private val connectionFactoryMap: Map<String, LettuceConnectionFactory> = connectionFactoryMap.toMap()

    /**
     * Looks up the [RedisTemplate] by name; returns null if not found (the caller is responsible for fallback or raising an error).
     */
    open fun getRedisTemplate(redisKey: String): RedisTemplate<Any, Any?>? = redisTemplateMap[redisKey]

    /**
     * Looks up the [RedisTemplate] by name; throws with the list of configured names if not found.
     * Prefer this over `getRedisTemplate(name)!!` so a misconfigured name fails with a diagnosable message.
     */
    open fun getRequiredRedisTemplate(redisKey: String): RedisTemplate<Any, Any?> =
        requireNotNull(redisTemplateMap[redisKey]) {
            "No RedisTemplate configured under kudos.ability.data.redis.redis-map for name [$redisKey]; " +
                    "configured names: ${redisTemplateMap.keys}"
        }

    /** Returns a read-only view of the name → template mapping. */
    open fun getRedisTemplateMap(): Map<String, RedisTemplate<Any, Any?>> = redisTemplateMap

    /**
     * Returns the [LettuceConnectionFactory] backing the named instance, or null when unknown (or when this
     * container was constructed without factories, e.g. in tests). Lets infrastructure that needs a raw
     * connection factory (message listener containers, Spring Session, health checks) reuse the managed one.
     */
    open fun getConnectionFactory(redisKey: String): LettuceConnectionFactory? = connectionFactoryMap[redisKey]

    /** Closes every managed connection factory; invoked by Spring when the context shuts down. */
    override fun destroy() {
        connectionFactoryMap.values.forEach { factory ->
            runCatching { factory.destroy() }
        }
    }

    companion object {
        /** Default key serializer (UTF-8 string); recommended for hash field names too. */
        val REDIS_KEY_SERIALIZER: StringRedisSerializer = StringRedisSerializer.UTF_8
    }
}
