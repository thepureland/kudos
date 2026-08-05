package io.kudos.ability.cache.common.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit test for the [ICacheConfigProvider.getHashCacheConfigs] default method.
 *
 * [io.kudos.ability.cache.common.support.DefaultCacheConfigProvider] overrides this method, so the
 * interface's default body (filtering all configs by `hash==true`) is exercised here through a minimal
 * implementation that does NOT override it.
 *
 * @author K
 * @since 1.0.0
 */
internal class ICacheConfigProviderTest {

    /** Minimal provider that relies on the interface default for getHashCacheConfigs(). */
    private class MinimalProvider(private val all: Map<String, CacheConfig>) : ICacheConfigProvider {
        override fun getCacheConfig(name: String): CacheConfig? = all[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = all
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        // getHashCacheConfigs intentionally NOT overridden
    }

    @Test
    fun defaultGetHashCacheConfigs_filtersByHashFlag() {
        val plain = CacheConfig().apply { name = "PLAIN"; hash = false }
        val hashed = CacheConfig().apply { name = "DICT"; hash = true }
        val provider = MinimalProvider(mapOf("PLAIN" to plain, "DICT" to hashed))

        val hashConfigs = provider.getHashCacheConfigs()
        assertEquals(setOf("DICT"), hashConfigs.keys)
        assertTrue(hashConfigs.values.all { it.hash })
    }

    @Test
    fun defaultGetHashCacheConfigs_noHashEntries_returnsEmpty() {
        val provider = MinimalProvider(mapOf("A" to CacheConfig().apply { name = "A" }))
        assertTrue(provider.getHashCacheConfigs().isEmpty())
    }
}
