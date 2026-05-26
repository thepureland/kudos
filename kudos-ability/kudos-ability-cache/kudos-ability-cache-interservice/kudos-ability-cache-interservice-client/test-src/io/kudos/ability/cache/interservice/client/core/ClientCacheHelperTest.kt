package io.kudos.ability.cache.interservice.client.core

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.interservice.client.init.InterServiceCacheClientProperties
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests configurable TTL and lenient local-cache reads in [ClientCacheHelper].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheHelperTest {

    @Test
    fun afterPropertiesSet_usesConfiguredTtl() {
        val cacheManager = RecordingCacheManager()
        val helper = ClientCacheHelper(
            InterServiceCacheClientProperties().apply { ttlSeconds = 42 },
            cacheManager
        )

        helper.afterPropertiesSet()

        val config = cacheManager.initializedConfigs[ClientCacheKey.FEIGN_CACHE_PREFIX]
        assertEquals(ClientCacheKey.FEIGN_CACHE_PREFIX, config?.name)
        assertEquals(true, config?.ignoreVersion)
        assertEquals(42, config?.ttl)
    }

    @Test
    fun afterPropertiesSet_rejectsNonPositiveTtl() {
        val helper = ClientCacheHelper(
            InterServiceCacheClientProperties().apply { ttlSeconds = 0 },
            RecordingCacheManager()
        )

        assertFailsWith<IllegalArgumentException> { helper.afterPropertiesSet() }
    }

    @Test
    fun loadFromLocalCache_evictsUnexpectedValueType() {
        val cacheManager = RecordingCacheManager()
        val helper = ClientCacheHelper(InterServiceCacheClientProperties(), cacheManager)
        helper.afterPropertiesSet()
        cacheManager.cache.put("cache-1", "not-a-client-cache-item")

        val item = helper.loadFromLocalCache("cache-1")

        assertNull(item)
        assertNull(cacheManager.cache.get("cache-1"))
    }

    @Test
    fun loadFromLocalCache_returnsClientCacheItem() {
        val cacheManager = RecordingCacheManager()
        val helper = ClientCacheHelper(InterServiceCacheClientProperties(), cacheManager)
        helper.afterPropertiesSet()
        cacheManager.cache.put("cache-1", ClientCacheItem("uid-1", "payload"))

        val item = helper.loadFromLocalCache("cache-1")

        assertEquals("uid-1", item?.uuid)
        assertEquals("payload", item?.cacheData)
    }

    /**
     * In-memory cache manager that records initialization configs, used to isolate the test from
     * the real Caffeine / Spring container.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingCacheManager : IKeyValueCacheManager<ConcurrentMapCache> {
        val cache = ConcurrentMapCache(ClientCacheKey.FEIGN_CACHE_PREFIX)
        var initializedConfigs: Map<String, CacheConfig> = emptyMap()

        override fun createCache(cacheConfig: CacheConfig): ConcurrentMapCache = cache

        override fun evictByPattern(cacheName: String, pattern: String) = Unit

        override fun existsKey(cacheName: String, key: Any): Boolean = cache.get(key) != null

        override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
            initializedConfigs = cacheConfigMap
        }

        override fun getCache(name: String): Cache? = cache.takeIf { name == it.name }

        override fun getCacheNames(): MutableCollection<String> = mutableListOf(cache.name)
    }
}
