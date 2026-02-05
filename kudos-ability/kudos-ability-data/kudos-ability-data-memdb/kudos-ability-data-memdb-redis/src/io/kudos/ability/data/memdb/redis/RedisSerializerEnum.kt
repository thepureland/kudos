package io.kudos.ability.data.memdb.redis

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer


/**
 * Redis序列化字典表
 */
enum class RedisSerializerEnum(
    val type: String,
    val serializerClazz: Class<out RedisSerializer<*>>
) {

    STRING("string", StringRedisSerializer::class.java),
    JDK("jdk", JdkSerializationRedisSerializer::class.java),

    //    JACKSON("jackson", GenericJackson2JsonRedisSerializer.class),
    FASTJSON("fastjson", GenericFastJsonRedisSerializer::class.java);

    companion object {
        fun ofEnum(type: String?): RedisSerializerEnum? {
            val enums = entries.toTypedArray()
            for (anEnum in enums) {
                if (anEnum.type == type) {
                    return anEnum
                }
            }
            return null
        }
    }
}
