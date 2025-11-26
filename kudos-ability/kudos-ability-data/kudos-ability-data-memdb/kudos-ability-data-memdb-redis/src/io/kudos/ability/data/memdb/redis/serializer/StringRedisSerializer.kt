package io.kudos.ability.data.memdb.redis.serializer

import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.nio.charset.StandardCharsets

class StringRedisSerializer(prefix: String?) : RedisSerializer<String> {

    private val prefix: String = "$prefix:"

    private val delegate: StringRedisSerializer = StringRedisSerializer(StandardCharsets.UTF_8)

    override fun serialize(key: String?): ByteArray {
        return delegate.serialize(prefix + key)
    }

    override fun deserialize(bytes: ByteArray?): String? {
        val key: String? = delegate.deserialize(bytes)
        if (key != null && key.startsWith(prefix)) {
            return key.substring(prefix.length)
        }
        return key
    }

}
