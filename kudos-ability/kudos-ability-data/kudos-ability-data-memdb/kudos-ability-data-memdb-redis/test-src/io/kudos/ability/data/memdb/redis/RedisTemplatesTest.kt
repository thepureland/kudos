package io.kudos.ability.data.memdb.redis

import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisTemplates] container behavior.
 *
 * Covers: lookup by name (hit / miss / required), the read-only map view, connection factory lookup and
 * shutdown via [RedisTemplates.destroy], and the companion default key serializer constant.
 * Pure unit test, no Redis required (connection factories are built lazily and never connect).
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisTemplatesTest {

    private fun newTemplates(): Triple<RedisTemplates, RedisTemplate<Any, Any?>, RedisTemplate<Any, Any?>> {
        val a = RedisTemplate<Any, Any?>()
        val b = RedisTemplate<Any, Any?>()
        val templates = RedisTemplates(mutableMapOf("a" to a, "b" to b), a)
        return Triple(templates, a, b)
    }

    @Test
    fun getRedisTemplate_byName_returnsRegisteredInstance() {
        val (templates, a, b) = newTemplates()
        assertSame(a, templates.getRedisTemplate("a"))
        assertSame(b, templates.getRedisTemplate("b"))
    }

    @Test
    fun getRedisTemplate_missingName_returnsNull() {
        val (templates, _, _) = newTemplates()
        assertNull(templates.getRedisTemplate("missing"))
        assertNull(templates.getRedisTemplate(""))
    }

    @Test
    fun getRequiredRedisTemplate_missingName_throwsListingConfiguredNames() {
        val (templates, a, _) = newTemplates()
        assertSame(a, templates.getRequiredRedisTemplate("a"))
        val ex = assertFailsWith<IllegalArgumentException> { templates.getRequiredRedisTemplate("missing") }
        assertTrue(ex.message!!.contains("missing"), ex.message)
        assertTrue(ex.message!!.contains("a"), "message should list configured names: ${ex.message}")
    }

    @Test
    fun getRedisTemplateMap_returnsFullMapping() {
        val (templates, a, b) = newTemplates()
        val map = templates.getRedisTemplateMap()
        assertEquals(2, map.size)
        assertSame(a, map["a"])
        assertSame(b, map["b"])
    }

    @Test
    fun connectionFactories_areLookedUpByName_andClosedOnDestroy() {
        val factory = RedisConnectFactory.newLettuceConnectionFactory(RedisExtProperties().apply {
            host = "localhost"
            port = 6379
        })
        val template = RedisTemplate<Any, Any?>()
        val templates = RedisTemplates(mapOf("main" to template), template, mapOf("main" to factory))
        assertSame(factory, templates.getConnectionFactory("main"))
        assertNull(templates.getConnectionFactory("missing"))
        templates.destroy()
        assertTrue(!factory.isRunning, "destroy() must shut the managed connection factory down")
    }

    @Test
    fun companion_redisKeySerializer_isUtf8Singleton() {
        assertSame(StringRedisSerializer.UTF_8, RedisTemplates.REDIS_KEY_SERIALIZER)
    }
}
