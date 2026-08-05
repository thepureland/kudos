package io.kudos.ability.cache.remote.redis

import io.kudos.context.kit.SpringKit
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.core.RedisTemplate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [ScanClearRedisCache] and [findRedisTemplate].
 *
 * Manipulates [SpringKit.applicationContext] with throw-away [GenericApplicationContext] instances
 * (saved and restored around each test) so all branches run without a real Redis:
 * - findRedisTemplate: prefers `stringRedisTemplate`, falls back to the first other template, null when none
 * - clear: keys+delete when a template exists, no delete on empty match, and the `super.clear()`
 *   (cacheWriter.clean) fallback when no template bean is present
 *
 * @author K
 * @since 1.0.0
 */
internal class ScanClearRedisCacheTest {

    private var previousContext: ApplicationContext? = null
    private val openedContexts = mutableListOf<GenericApplicationContext>()

    @BeforeTest
    fun saveContext() {
        previousContext = runCatching { SpringKit.applicationContext }.getOrNull()
    }

    @AfterTest
    fun restoreContext() {
        SpringKit.applicationContext = previousContext
        openedContexts.forEach { it.close() }
        openedContexts.clear()
    }

    private fun installContext(vararg beans: Pair<String, Any>) {
        val ctx = GenericApplicationContext()
        beans.forEach { (name, bean) -> ctx.beanFactory.registerSingleton(name, bean) }
        ctx.refresh()
        openedContexts += ctx
        SpringKit.applicationContext = ctx
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockTemplate(): RedisTemplate<String, Any> =
        mock(RedisTemplate::class.java) as RedisTemplate<String, Any>

    private fun newCache(cacheWriter: RedisCacheWriter = mock(RedisCacheWriter::class.java)): ScanClearRedisCache =
        ScanClearRedisCache("testCache", cacheWriter, RedisCacheConfiguration.defaultCacheConfig())

    // ---------- findRedisTemplate ----------

    @Test
    fun findRedisTemplate_prefersStringRedisTemplate() {
        val preferred = mockTemplate()
        val other = mockTemplate()
        installContext("aOtherTemplate" to other, "stringRedisTemplate" to preferred)
        assertSame(preferred, findRedisTemplate())
    }

    @Test
    fun findRedisTemplate_fallsBackToAnyTemplate() {
        val only = mockTemplate()
        installContext("someRedisTemplate" to only)
        assertSame(only, findRedisTemplate())
    }

    @Test
    fun findRedisTemplate_returnsNullWhenNoTemplateBean() {
        installContext() // empty context
        assertNull(findRedisTemplate())
    }

    // ---------- clear ----------

    @Test
    fun clear_deletesAllKeysMatchingCachePrefix() {
        val template = mockTemplate()
        // default key prefix for cache "testCache" is "testCache::"
        val matched = setOf("testCache::a", "testCache::b")
        `when`(template.keys("testCache::*")).thenReturn(matched)
        installContext("stringRedisTemplate" to template)

        newCache().clear()

        verify(template).delete(matched)
    }

    @Test
    fun clear_noMatchedKeys_skipsDelete() {
        val template = mockTemplate()
        `when`(template.keys("testCache::*")).thenReturn(emptySet())
        installContext("stringRedisTemplate" to template)

        newCache().clear()

        verify(template, never()).delete(ArgumentMatchers.anyCollection())
    }

    @Test
    fun clear_withoutTemplateBean_fallsBackToSpringDefaultClear() {
        installContext() // no RedisTemplate beans
        val cacheWriter = mock(RedisCacheWriter::class.java)

        newCache(cacheWriter).clear()

        // Spring's default RedisCache.clear() path delegates to cacheWriter.clear(name, pattern)
        verify(cacheWriter).clear(ArgumentMatchers.eq("testCache"), ArgumentMatchers.any(ByteArray::class.java))
    }
}
