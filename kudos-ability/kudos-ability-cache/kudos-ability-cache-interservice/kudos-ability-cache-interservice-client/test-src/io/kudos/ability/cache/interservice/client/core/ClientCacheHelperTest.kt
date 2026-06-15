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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests configurable TTL and lenient local-cache reads in [ClientCacheHelper].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheHelperTest {

    @Test
    fun hasLocalCache_reflectsCacheManagerPresence() {
        assertTrue(ClientCacheHelper(InterServiceCacheClientProperties(), RecordingCacheManager()).hasLocalCache())
        assertFalse(ClientCacheHelper().hasLocalCache())
        assertFalse(ClientCacheHelper(InterServiceCacheClientProperties(), null).hasLocalCache())
    }

    @Test
    @Suppress("DEPRECATION")
    fun havaLocalCache_delegatesToHasLocalCache() {
        assertTrue(ClientCacheHelper(InterServiceCacheClientProperties(), RecordingCacheManager()).havaLocalCache())
        assertFalse(ClientCacheHelper().havaLocalCache())
    }

    @Test
    fun afterPropertiesSet_withoutLocalCache_disablesFeatureSilently() {
        // Even an invalid TTL must not fail when no local cache manager is present: feature is disabled.
        val helper = ClientCacheHelper(InterServiceCacheClientProperties().apply { ttlSeconds = 0 }, null)

        helper.afterPropertiesSet()

        // Cache access without a manager fails fast with a clear message.
        val ex = assertFailsWith<IllegalStateException> { helper.loadFromLocalCache("any-key") }
        assertEquals("localCacheManager not available", ex.message)
    }

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

        val ex = assertFailsWith<IllegalArgumentException> { helper.afterPropertiesSet() }
        assertTrue(
            ex.message!!.contains("ttl-seconds must be greater than 0"),
            "error message should point at the ttl-seconds property, actual: ${ex.message}"
        )
    }

    @Test
    fun afterPropertiesSet_rejectsNegativeTtl() {
        val helper = ClientCacheHelper(
            InterServiceCacheClientProperties().apply { ttlSeconds = -1 },
            RecordingCacheManager()
        )

        assertFailsWith<IllegalArgumentException> { helper.afterPropertiesSet() }
    }

    @Test
    fun afterPropertiesSet_failsFastWhenSubclassClaimsLocalCacheButManagerIsNull() {
        // Defensive guard branch: a subclass may override hasLocalCache() to true while the manager is absent.
        val helper = object : ClientCacheHelper(InterServiceCacheClientProperties(), null) {
            override fun hasLocalCache(): Boolean = true
        }

        val ex = assertFailsWith<IllegalArgumentException> { helper.afterPropertiesSet() }
        assertTrue(
            ex.message!!.contains("localCacheManager not available"),
            "error message should explain the missing manager, actual: ${ex.message}"
        )
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

    @Test
    fun loadFromLocalCache_returnsNullWhenEntryAbsent() {
        val cacheManager = RecordingCacheManager()
        val helper = ClientCacheHelper(InterServiceCacheClientProperties(), cacheManager)
        helper.afterPropertiesSet()

        assertNull(helper.loadFromLocalCache("missing-key"))
    }

    @Test
    fun writeToLocalCache_putsEntryReadableViaLoad() {
        val cacheManager = RecordingCacheManager()
        val helper = ClientCacheHelper(InterServiceCacheClientProperties(), cacheManager)
        helper.afterPropertiesSet()

        helper.writeToLocalCache("cache-w", ClientCacheItem("uid-w", "data-w"))

        val item = helper.loadFromLocalCache("cache-w")
        assertEquals("uid-w", item?.uuid)
        assertEquals("data-w", item?.cacheData)
    }

    @Test
    fun writeToLocalCache_nullData_yieldsNullOnLoad() {
        val cacheManager = RecordingCacheManager()
        val helper = ClientCacheHelper(InterServiceCacheClientProperties(), cacheManager)
        helper.afterPropertiesSet()

        helper.writeToLocalCache("cache-null", null)

        assertNull(helper.loadFromLocalCache("cache-null"))
    }

    @Test
    fun localCacheAccess_failsFastWhenRegionNotInitialized() {
        val cacheManager = RecordingCacheManager().apply { cacheAvailable = false }
        val helper = ClientCacheHelper(InterServiceCacheClientProperties(), cacheManager)

        val ex = assertFailsWith<IllegalStateException> {
            helper.writeToLocalCache("k", ClientCacheItem("u", "d"))
        }
        assertTrue(
            ex.message!!.contains("not initialized"),
            "error message should explain that the Feign cache region is uninitialized, actual: ${ex.message}"
        )
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
        var cacheAvailable = true

        override fun createCache(cacheConfig: CacheConfig): ConcurrentMapCache = cache

        override fun evictByPattern(cacheName: String, pattern: String) = Unit

        override fun existsKey(cacheName: String, key: Any): Boolean = cache.get(key) != null

        override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
            initializedConfigs = cacheConfigMap
        }

        override fun getCache(name: String): Cache? =
            if (cacheAvailable) cache.takeIf { name == it.name } else null

        override fun getCacheNames(): MutableCollection<String> = mutableListOf(cache.name)
    }
}
