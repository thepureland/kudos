package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AbstractKeyValueCacheHandler] default methods: isExists / value / evict / clear / reload.
 *
 * Uses [KeyValueCacheKit.overrideForTesting] so no real cache infrastructure is needed.
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractKeyValueCacheHandlerTest {

    private val cacheName = "KV_HANDLER_TEST"
    private lateinit var cacheManager: TestMixCacheManager
    private lateinit var provider: TestProvider

    @BeforeTest
    fun setup() {
        cacheManager = TestMixCacheManager()
        provider = TestProvider().apply { register(cacheName, active = true) }
        KeyValueCacheKit.overrideForTesting(cacheManager, provider)
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
    }

    @Test
    fun value_returnsCachedValue() {
        cacheManager.getOrCreate(cacheName).put("k", "v")
        val handler = StringHandler(cacheName)
        assertEquals("v", handler.value("k"))
    }

    @Test
    fun value_returnsNullWhenAbsent() {
        val handler = StringHandler(cacheName)
        assertNull(handler.value("missing"))
    }

    @Test
    fun isExists_reflectsPresence() {
        val handler = StringHandler(cacheName)
        assertFalse(handler.isExists("k"))
        cacheManager.getOrCreate(cacheName).put("k", "v")
        assertTrue(handler.isExists("k"))
    }

    @Test
    fun evict_removesEntry() {
        cacheManager.getOrCreate(cacheName).put("k", "v")
        val handler = StringHandler(cacheName)
        handler.evict("k")
        assertNull(cacheManager.getOrCreate(cacheName).get("k"))
    }

    @Test
    fun clear_removesAllEntries() {
        val cache = cacheManager.getOrCreate(cacheName)
        cache.put("a", "1")
        cache.put("b", "2")
        val handler = StringHandler(cacheName)
        handler.clear()
        assertNull(cache.get("a"))
        assertNull(cache.get("b"))
    }

    @Test
    fun reload_evictsThenLoads_whenDoReloadHasValue() {
        cacheManager.getOrCreate(cacheName).put("k", "stale")
        val handler = StringHandler(cacheName, reloadValue = "fresh")
        handler.reload("k")
        // reload evicts; the cached entry must be gone (doReload here only signals presence)
        assertNull(cacheManager.getOrCreate(cacheName).get("k"))
        assertEquals(1, handler.doReloadCalls)
    }

    @Test
    fun reload_handlesNullDoReload() {
        cacheManager.getOrCreate(cacheName).put("k", "stale")
        val handler = StringHandler(cacheName, reloadValue = null)
        handler.reload("k")
        assertNull(cacheManager.getOrCreate(cacheName).get("k"))
        assertEquals(1, handler.doReloadCalls)
    }

    // ---- helpers ----

    private open class StringHandler(
        private val name: String,
        private val reloadValue: String? = null,
    ) : AbstractKeyValueCacheHandler<String>() {
        var doReloadCalls = 0
        override fun cacheName(): String = name
        override fun reloadAll(clear: Boolean) {}
        override fun doReload(key: String): String? {
            doReloadCalls++
            return reloadValue
        }
    }

    /**
     * Returns a REMOTE-strategy [MixCache] from [getCache] so that [KeyValueCacheKit.evict]/clear go
     * straight through `doEvict`/`doClear` (no distributed notification). [getOrCreate] exposes the
     * underlying [ConcurrentMapCache] for test seeding / assertions.
     */
    private class TestMixCacheManager : MixCacheManager() {
        private val underlying = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        fun getOrCreate(name: String): Cache = underlying.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache =
            wrappers.computeIfAbsent(name) { MixCache(CacheStrategy.REMOTE, null, getOrCreate(it)) }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean) {
            configs[name] = CacheConfig().apply { this.name = name; this.active = active }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
