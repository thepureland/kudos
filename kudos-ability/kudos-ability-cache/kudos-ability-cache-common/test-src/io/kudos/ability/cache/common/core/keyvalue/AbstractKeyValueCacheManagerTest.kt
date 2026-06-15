package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.support.CacheConfig
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AbstractKeyValueCacheManager]: createCache-per-config initialization, add(), and
 * loadCaches() returning the accumulated cache list.
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractKeyValueCacheManagerTest {

    @Test
    fun initCacheAfterSystemInit_createsAndRegistersOneCachePerConfig() {
        val mgr = TestManager()
        mgr.initCacheAfterSystemInit(
            mapOf(
                "a" to CacheConfig().apply { name = "a" },
                "b" to CacheConfig().apply { name = "b" }
            )
        )
        assertEquals(2, mgr.caches.size)
        assertEquals(setOf("a", "b"), mgr.exposeLoadCaches().map { it.name }.toSet())
        // getCache works after afterPropertiesSet was called internally.
        assertTrue(mgr.cacheNames.containsAll(setOf("a", "b")))
    }

    @Test
    fun add_appendsToCacheList() {
        val mgr = TestManager()
        val c = ConcurrentMapCache("x", true)
        mgr.add(c)
        assertEquals(listOf<Cache>(c), mgr.caches)
        assertEquals(listOf<Cache>(c), mgr.exposeLoadCaches().toList())
    }

    @Test
    fun initWithEmptyMap_yieldsNoCaches() {
        val mgr = TestManager()
        mgr.initCacheAfterSystemInit(emptyMap())
        assertTrue(mgr.caches.isEmpty())
    }

    /** Concrete manager that creates a ConcurrentMapCache named after the config. */
    private class TestManager : AbstractKeyValueCacheManager<Cache>() {
        override fun createCache(cacheConfig: CacheConfig): Cache =
            ConcurrentMapCache(cacheConfig.name ?: "?", true)
        override fun evictByPattern(cacheName: String, pattern: String) {}
        override fun existsKey(cacheName: String, key: Any): Boolean = false
        fun exposeLoadCaches(): MutableCollection<out Cache> = loadCaches()
    }
}
