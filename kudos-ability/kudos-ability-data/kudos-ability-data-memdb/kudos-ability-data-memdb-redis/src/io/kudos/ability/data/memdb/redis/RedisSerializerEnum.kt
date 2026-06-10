package io.kudos.ability.data.memdb.redis

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer


/**
 * Redis serializer dictionary. When configuring fields such as
 * `kudos.ability.data.redis.redis-map.<name>.key-serializer`, fill in the [type] literal of this enum.
 * At runtime, [RedisExtProperties.getSerializerByType] reflectively constructs the corresponding [RedisSerializer].
 *
 * Currently supported:
 *  - [STRING] — UTF-8 string serialization
 *  - [JDK] — Native Java `Serializable`; compact binary but strongly coupled across service versions
 *  - [FASTJSON] — Fastjson2 JSON serialization; highly readable and cross-language friendly
 *
 * To add a new serializer, add an enum value here; `getSerializerByType` instantiates it
 * reflectively (the class must expose a public no-arg constructor).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class RedisSerializerEnum(
    /** Literal identifier used in yml configuration. */
    val type: String,
    /** Corresponding Spring Data Redis serializer type. */
    val serializerClazz: Class<out RedisSerializer<*>>
) {

    /** UTF-8 string serializer. */
    STRING("string", StringRedisSerializer::class.java),

    /** JDK serializer (requires the object to implement `Serializable`). */
    JDK("jdk", JdkSerializationRedisSerializer::class.java),

    /** Fastjson2 JSON serializer; recommended for data shared across services. */
    FASTJSON("fastjson", GenericFastJsonRedisSerializer::class.java);

    companion object {
        /**
         * Looks up the enum by the [type] literal; returns null if no match is found.
         */
        fun ofEnum(type: String?): RedisSerializerEnum? = entries.find { it.type == type }
    }
}
