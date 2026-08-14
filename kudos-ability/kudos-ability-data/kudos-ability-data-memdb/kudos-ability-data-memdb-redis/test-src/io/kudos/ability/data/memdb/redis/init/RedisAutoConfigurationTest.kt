package io.kudos.ability.data.memdb.redis.init

import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisAutoConfiguration] bean factory methods.
 *
 * Covers: template-map assembly (serializers wired from configuration, default template selection),
 * the two requireNotNull failure paths (missing default-redis / unknown default key), the
 * redisTemplate bean delegation and the component name. The Lettuce connection factory is created
 * lazily, so no real Redis is needed.
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisAutoConfigurationTest {

    private val configuration = RedisAutoConfiguration()

    @Test
    fun redisProperties_returnsFreshInstance() {
        val first = configuration.redisProperties()
        val second = configuration.redisProperties()
        assertNotNull(first)
        assertNotSame(first, second)
    }

    @Test
    fun redisTemplateMap_buildsTemplatePerEntry_andWiresSerializers() {
        val properties = RedisProperties().apply {
            defaultRedis = "main"
            redisMap["main"] = RedisExtProperties().apply {
                host = "localhost"
                port = 6379
            }
            redisMap["cache"] = RedisExtProperties().apply {
                host = "localhost"
                port = 6380
                keySerializer = "string"
                hashkeySerializer = "string"
                valueSerializer = "fastjson"
                hashvalueSerializer = "fastjson"
            }
        }
        val templates = configuration.redisTemplateMap(properties)
        try {
            assertEquals(2, templates.getRedisTemplateMap().size)
            assertSame(templates.getRedisTemplate("main"), templates.defaultRedisTemplate)

            val main = assertNotNull(templates.getRedisTemplate("main"))
            // defaults: key=string -> UTF_8 singleton, others jdk
            assertSame(StringRedisSerializer.UTF_8, main.keySerializer)
            assertTrue(main.hashKeySerializer is JdkSerializationRedisSerializer)
            assertTrue(main.valueSerializer is JdkSerializationRedisSerializer)
            assertTrue(main.hashValueSerializer is JdkSerializationRedisSerializer)

            val cache = assertNotNull(templates.getRedisTemplate("cache"))
            assertSame(StringRedisSerializer.UTF_8, cache.hashKeySerializer)
            assertEquals(
                "GenericFastJsonRedisSerializer",
                cache.valueSerializer!!.javaClass.simpleName
            )

            // the per-instance connection factories are retained for lifecycle management and reuse
            assertSame(
                templates.getRedisTemplate("main")!!.connectionFactory as LettuceConnectionFactory,
                assertNotNull(templates.getConnectionFactory("main"))
            )
            assertNotNull(templates.getConnectionFactory("cache"))

            // redisTemplate bean simply exposes the default template
            assertSame(templates.defaultRedisTemplate, configuration.redisTemplate(templates))

            // the rate limiter aspect is registered by this configuration (not component scanning)
            assertNotNull(configuration.rateLimiterAspect(templates))
        } finally {
            // DisposableBean contract: shuts down every managed connection factory
            templates.destroy()
        }
    }

    @Test
    fun redisTemplateMap_missingDefaultRedis_throws() {
        val properties = RedisProperties() // defaultRedis stays null
        val ex = assertFailsWith<IllegalArgumentException> { configuration.redisTemplateMap(properties) }
        assertTrue(ex.message!!.contains("default-redis must be set"), ex.message)
    }

    @Test
    fun redisTemplateMap_unknownDefaultKey_throws() {
        val properties = RedisProperties().apply { defaultRedis = "nowhere" }
        val ex = assertFailsWith<IllegalArgumentException> { configuration.redisTemplateMap(properties) }
        assertTrue(ex.message!!.contains("no redis config for default-redis: nowhere"), ex.message)
    }

    @Test
    fun componentName_isModuleName() {
        assertEquals("kudos-ability-data-memdb-redis", configuration.getComponentName())
    }
}
