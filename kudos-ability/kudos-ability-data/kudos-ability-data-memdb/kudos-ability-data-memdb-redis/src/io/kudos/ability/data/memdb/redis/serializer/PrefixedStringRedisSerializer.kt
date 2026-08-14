package io.kudos.ability.data.memdb.redis.serializer

import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.nio.charset.StandardCharsets

/**
 * UTF-8 serializer that automatically adds/removes a prefix for String keys.
 *
 * Use case: when business requires all keys to carry a namespace (e.g. `tenant1:`), install this
 * serializer on the RedisTemplate's `keySerializer`. Business code can continue to use bare keys;
 * the prefix is added automatically on write to Redis and stripped on read.
 *
 * On deserialization, if the [prefix] is absent, the original value is preserved to remain
 * compatible with legacy unprefixed data.
 *
 * (Formerly named `StringRedisSerializer`; renamed so it can no longer be confused with Spring's
 * built-in [org.springframework.data.redis.serializer.StringRedisSerializer].)
 *
 * @param prefix namespace prefix, without the trailing `:` (appended automatically); must not be blank
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class PrefixedStringRedisSerializer(prefix: String) : RedisSerializer<String> {

    init {
        require(prefix.isNotBlank()) { "prefix must not be blank" }
    }

    private val prefix: String = "$prefix:"

    private val delegate: StringRedisSerializer = StringRedisSerializer(StandardCharsets.UTF_8)

    /**
     * Serialize: automatically prepends [prefix]. The interface requires a non-null result, so a null key is
     * treated as the empty key (bare prefix) — never rendered as the literal string "null".
     */
    override fun serialize(value: String?): ByteArray = delegate.serialize(prefix + value.orEmpty())

    /** Deserialize: strips [prefix] if present, otherwise preserves the original value (compatible with legacy unprefixed data). */
    override fun deserialize(bytes: ByteArray?): String? {
        val key = delegate.deserialize(bytes) ?: return null
        return key.removePrefix(prefix).takeIf { it.length < key.length } ?: key
    }

}
