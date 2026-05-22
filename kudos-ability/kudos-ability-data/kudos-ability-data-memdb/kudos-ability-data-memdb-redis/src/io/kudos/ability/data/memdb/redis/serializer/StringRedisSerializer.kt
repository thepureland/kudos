package io.kudos.ability.data.memdb.redis.serializer

import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.nio.charset.StandardCharsets

/**
 * 给 String key 自动加 / 去前缀的 UTF-8 序列化器。
 *
 * 用途：业务希望全部 key 都带 namespace（如 `tenant1:`），把本序列化器装到 RedisTemplate 的
 * `keySerializer` 上即可，业务代码继续用裸 key，落到 Redis 时自动拼前缀，读出时再剥掉。
 *
 * 反序列化时 `removePrefix` 不命中（[prefix] 不存在）保留原值——为了兼容历史无前缀数据。
 *
 * 注：本类与 Spring 自带的 [org.springframework.data.redis.serializer.StringRedisSerializer]
 * 同名，不同包；引用时务必区分。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class StringRedisSerializer(prefix: String?) : RedisSerializer<String> {

    private val prefix: String = "$prefix:"

    private val delegate: StringRedisSerializer = StringRedisSerializer(StandardCharsets.UTF_8)

    /** 序列化：自动拼上 [prefix]。 */
    override fun serialize(key: String?): ByteArray = delegate.serialize(prefix + key)

    /** 反序列化：命中 [prefix] 则剥掉，否则保留原值（兼容旧无前缀数据）。 */
    override fun deserialize(bytes: ByteArray?): String? {
        val key = delegate.deserialize(bytes) ?: return null
        return key.removePrefix(prefix).takeIf { it.length < key.length } ?: key
    }

}
