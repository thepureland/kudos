package io.kudos.ability.data.memdb.redis.init.properties

import io.kudos.ability.data.memdb.redis.RedisSerializerEnum
import org.springframework.beans.BeanUtils
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Extended configuration for a single Redis instance. Extends Spring Boot's [DataRedisProperties] (which provides
 * host / port / database / password / cluster / ssl, etc.) and layers on top:
 *  - Four serializer choices ([keySerializer] / [hashkeySerializer] / [valueSerializer] / [hashvalueSerializer]).
 *  - Apache commons-pool2 connection pool parameters ([maxIdle] / [minIdle] / [maxActive] / [maxWait]).
 *
 * Serializer field values must match [RedisSerializerEnum.type] literals; unmatched values throw at startup.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisExtProperties : DataRedisProperties() {
    /** Key serializer type; defaults to [RedisSerializerEnum.STRING]. */
    var keySerializer: String? = RedisSerializerEnum.STRING.type

    /** Hash field serializer type; defaults to [RedisSerializerEnum.JDK]. */
    var hashkeySerializer: String? = RedisSerializerEnum.JDK.type

    /** Value serializer type; defaults to [RedisSerializerEnum.JDK]. */
    var valueSerializer: String? = RedisSerializerEnum.JDK.type

    /** Hash value serializer type; defaults to [RedisSerializerEnum.JDK]. */
    var hashvalueSerializer: String? = RedisSerializerEnum.JDK.type

    /** Maximum number of idle connections in the pool. */
    var maxIdle: Int = 8

    /**
     * Target for the minimum number of idle connections to maintain in the pool. This
     * setting only has an effect if both it and time between eviction runs are
     * positive.
     */
    var minIdle: Int = 0

    /**
     * Maximum number of connections that can be allocated by the pool at a given
     * time. Use a negative value for no limit.
     */
    var maxActive: Int = 8

    /**
     * Maximum amount of time a connection allocation should block before throwing an
     * exception when the pool is exhausted. Use a negative value to block
     * indefinitely.
     */
    var maxWait: Duration? = Duration.ofMillis(-1)

    /** Resolves the [valueSerializer] configuration into an instance ready to attach to a RedisTemplate. */
    fun valueSerializer(): RedisSerializer<*> {
        return getSerializerByType(this.valueSerializer)
    }

    /** Resolves the [hashvalueSerializer]. */
    fun hashvalueSerializer(): RedisSerializer<*> {
        return getSerializerByType(this.hashvalueSerializer)
    }

    /** Resolves the [keySerializer]. */
    fun keySerializer(): RedisSerializer<*> {
        return getSerializerByType(this.keySerializer)
    }

    /** Resolves the [hashkeySerializer]. */
    fun hashkeySerializer(): RedisSerializer<*> {
        return getSerializerByType(this.hashkeySerializer)
    }

    /**
     * Instantiates the corresponding serializer by the literal [type].
     * Special handling: [StringRedisSerializer] does not go through `instantiateClass`; the singleton `UTF_8` instance is reused.
     * Unrecognized types throw immediately (failing at startup is preferable to an unexpected runtime error).
     */
    private fun getSerializerByType(type: String?): RedisSerializer<*> {
        val redisSerializerEnum = RedisSerializerEnum.ofEnum(type)
            ?: throw RuntimeException(
                "The specified redisSerializer [$type] does not exist in the RedisSerializerEnum enum! " +
                        "Valid values: ${RedisSerializerEnum.entries.joinToString { it.type }}"
            )
        val serializerClazz = redisSerializerEnum.serializerClazz
        if (serializerClazz.isAssignableFrom(StringRedisSerializer::class.java)) {
            return StringRedisSerializer.UTF_8
        }
        return BeanUtils.instantiateClass(serializerClazz)
    }
}
