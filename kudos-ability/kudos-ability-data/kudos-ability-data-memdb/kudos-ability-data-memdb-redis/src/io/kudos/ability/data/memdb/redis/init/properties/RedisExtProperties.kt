package io.kudos.ability.data.memdb.redis.init.properties

import io.kudos.ability.data.memdb.redis.RedisSerializerEnum
import org.springframework.beans.BeanUtils
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

class RedisExtProperties : RedisProperties() {
    var keySerializer: String? = RedisSerializerEnum.STRING.type
    var hashkeySerializer: String? = RedisSerializerEnum.JDK.type
    var valueSerializer: String? = RedisSerializerEnum.JDK.type
    var hashvalueSerializer: String? = RedisSerializerEnum.JDK.type

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

    fun valueSerializer(): RedisSerializer<*> {
        return getSerializerByType(this.valueSerializer)
    }

    fun hashvalueSerializer(): RedisSerializer<*> {
        return getSerializerByType(this.hashvalueSerializer)
    }

    fun keySerializer(): RedisSerializer<*> {
        return getSerializerByType(this.keySerializer)
    }

    fun hashkeySerializer(): RedisSerializer<*> {
        return getSerializerByType(this.hashkeySerializer)
    }

    private fun getSerializerByType(type: String?): RedisSerializer<*> {
        val redisSerializerEnum = RedisSerializerEnum.ofEnum(type)
        if (redisSerializerEnum == null) {
            throw RuntimeException("指定的redisSerializer不存在于RedisSerializerEnum枚举中！")
        }
        val serializerClazz = redisSerializerEnum.serializerClazz
        if (serializerClazz.isAssignableFrom(StringRedisSerializer::class.java)) {
            return StringRedisSerializer.UTF_8
        }
        return BeanUtils.instantiateClass(serializerClazz)
    }
}
