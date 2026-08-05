package io.kudos.ability.data.memdb.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [RedisTemplates] container behavior.
 *
 * Covers: lookup by name (hit / miss), the full internal map view, replacing the default template,
 * and the companion default key serializer constant. Pure unit test, no Redis required.
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
    fun getRedisTemplateMap_returnsFullMapping() {
        val (templates, a, b) = newTemplates()
        val map = templates.getRedisTemplateMap()
        assertEquals(2, map.size)
        assertSame(a, map["a"])
        assertSame(b, map["b"])
    }

    @Test
    fun defaultRedisTemplate_isMutable() {
        val (templates, a, b) = newTemplates()
        assertSame(a, templates.defaultRedisTemplate)
        templates.defaultRedisTemplate = b
        assertSame(b, templates.defaultRedisTemplate)
    }

    @Test
    fun companion_redisKeySerializer_isUtf8Singleton() {
        assertSame(StringRedisSerializer.UTF_8, RedisTemplates.REDIS_KEY_SERIALIZER)
    }
}
