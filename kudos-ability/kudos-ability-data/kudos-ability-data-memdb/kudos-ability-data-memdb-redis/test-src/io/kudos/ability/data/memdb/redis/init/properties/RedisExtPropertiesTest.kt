package io.kudos.ability.data.memdb.redis.init.properties

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer
import io.kudos.ability.data.memdb.redis.RedisSerializerEnum
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisExtProperties] serializer resolution and pool parameter defaults.
 *
 * Covers: the four default serializer literals, resolution of every [RedisSerializerEnum] type
 * (including the UTF_8 singleton special case for string), the exception path for unknown types,
 * and connection pool property accessors. Pure unit test, no Redis required.
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisExtPropertiesTest {

    @Test
    fun defaults_matchDocumentedLiterals() {
        val props = RedisExtProperties()
        assertEquals("string", props.keySerializer)
        assertEquals("jdk", props.hashkeySerializer)
        assertEquals("jdk", props.valueSerializer)
        assertEquals("jdk", props.hashvalueSerializer)
        assertEquals(8, props.maxIdle)
        assertEquals(0, props.minIdle)
        assertEquals(8, props.maxActive)
        assertEquals(Duration.ofMillis(-1), props.maxWait)
    }

    @Test
    fun keySerializer_string_reusesUtf8Singleton() {
        val props = RedisExtProperties()
        assertSame(StringRedisSerializer.UTF_8, props.keySerializer())
    }

    @Test
    fun jdkSerializers_areInstantiated() {
        val props = RedisExtProperties()
        assertTrue(props.hashkeySerializer() is JdkSerializationRedisSerializer)
        assertTrue(props.valueSerializer() is JdkSerializationRedisSerializer)
        assertTrue(props.hashvalueSerializer() is JdkSerializationRedisSerializer)
    }

    @Test
    fun fastjsonSerializer_isInstantiated() {
        val props = RedisExtProperties().apply {
            valueSerializer = "fastjson"
            hashvalueSerializer = "fastjson"
        }
        assertTrue(props.valueSerializer() is GenericFastJsonRedisSerializer)
        assertTrue(props.hashvalueSerializer() is GenericFastJsonRedisSerializer)
    }

    @Test
    fun stringSerializer_forEveryPosition() {
        val props = RedisExtProperties().apply {
            keySerializer = "string"
            hashkeySerializer = "string"
            valueSerializer = "string"
            hashvalueSerializer = "string"
        }
        assertSame(StringRedisSerializer.UTF_8, props.keySerializer())
        assertSame(StringRedisSerializer.UTF_8, props.hashkeySerializer())
        assertSame(StringRedisSerializer.UTF_8, props.valueSerializer())
        assertSame(StringRedisSerializer.UTF_8, props.hashvalueSerializer())
    }

    @Test
    fun unknownSerializerType_throwsWithHelpfulMessage() {
        val props = RedisExtProperties().apply { keySerializer = "kryo" }
        val ex = assertFailsWith<RuntimeException> { props.keySerializer() }
        assertTrue(ex.message!!.contains("kryo"), "message should mention the bad literal: ${ex.message}")
        assertTrue(ex.message!!.contains("string"), "message should list valid literals: ${ex.message}")
    }

    @Test
    fun nullSerializerType_throws() {
        val props = RedisExtProperties().apply { valueSerializer = null }
        assertFailsWith<RuntimeException> { props.valueSerializer() }
        assertNull(props.valueSerializer)
    }

    @Test
    fun poolProperties_areMutable() {
        val props = RedisExtProperties().apply {
            maxIdle = 16
            minIdle = 2
            maxActive = -1
            maxWait = Duration.ofSeconds(3)
        }
        assertEquals(16, props.maxIdle)
        assertEquals(2, props.minIdle)
        assertEquals(-1, props.maxActive)
        assertEquals(Duration.ofSeconds(3), props.maxWait)
    }
}
