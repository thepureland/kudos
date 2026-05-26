package io.kudos.ability.data.memdb.redis

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Regression tests for [RedisSerializerEnum] literal-value mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedisSerializerEnumTest {

    @Test
    fun ofEnum_matchesLiteralType() {
        assertEquals(RedisSerializerEnum.STRING, RedisSerializerEnum.ofEnum("string"))
        assertEquals(RedisSerializerEnum.JDK, RedisSerializerEnum.ofEnum("jdk"))
        assertEquals(RedisSerializerEnum.FASTJSON, RedisSerializerEnum.ofEnum("fastjson"))
    }

    @Test
    fun ofEnum_returnsNullForUnknown() {
        assertNull(RedisSerializerEnum.ofEnum("kryo"))
        assertNull(RedisSerializerEnum.ofEnum(null))
        assertNull(RedisSerializerEnum.ofEnum(""))
    }

    @Test
    fun ofEnum_caseSensitive() {
        // Upstream yml literal-value convention uses all lowercase; uppercase does not match
        assertNull(RedisSerializerEnum.ofEnum("STRING"))
        assertNull(RedisSerializerEnum.ofEnum("Jdk"))
    }

    @Test
    fun serializerClasses_areCorrect() {
        assertEquals(StringRedisSerializer::class.java, RedisSerializerEnum.STRING.serializerClazz)
        assertEquals(JdkSerializationRedisSerializer::class.java, RedisSerializerEnum.JDK.serializerClazz)
        assertEquals(GenericFastJsonRedisSerializer::class.java, RedisSerializerEnum.FASTJSON.serializerClazz)
    }
}
