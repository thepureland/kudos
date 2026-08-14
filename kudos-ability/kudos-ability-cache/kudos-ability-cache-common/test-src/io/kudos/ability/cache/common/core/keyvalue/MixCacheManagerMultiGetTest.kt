package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.CacheItemInitializing
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [MixCacheManager.multiGet].
 *
 * Bulk lookup exists so a batch of N keys costs one round trip rather than N; the contract that makes it
 * usable is that **a key present in the result was a hit even when its value is null**, which is what lets
 * negative caching work through `@BatchCacheable`.
 *
 * @author K
 * @since 1.0.0
 */
internal class MixCacheManagerMultiGetTest {

    // ---- 契约：命中以 key 是否存在为准，而非值是否为 null ----------------------

    @Test
    fun cachedNullIsAHit_notAMiss() {
        val local = RecordingKvManager().apply { put("l1", "k1", "v1"); put("l1", "k2", null) }
        val mgr = manager(local = local, configs = mapOf("l1" to CacheStrategy.SINGLE_LOCAL))

        val result = mgr.multiGet("l1", listOf("k1", "k2", "k3"))

        assertTrue(result.containsKey("k2"), "缓存的 null 必须算命中，否则每次都会重新回源")
        assertNull(result["k2"])
        assertEquals("v1", result["k1"])
        assertTrue(!result.containsKey("k3"), "未缓存的 key 不应出现在结果中")
    }

    // ---- 契约：确实是批量调用，而不是逐键循环 ---------------------------------

    @Test
    fun issuesOneBulkCallForTheWholeKeySet() {
        val local = RecordingKvManager().apply { put("l1", "k1", "v1"); put("l1", "k2", "v2") }
        val mgr = manager(local = local, configs = mapOf("l1" to CacheStrategy.SINGLE_LOCAL))

        mgr.multiGet("l1", listOf("k1", "k2", "k3"))

        assertEquals(
            listOf(listOf<Any>("k1", "k2", "k3")), local.multiGetCalls.map { it.second },
            "整批 key 应一次性下推，逐键读取正是 multiGet 要消除的成本"
        )
    }

    // ---- LOCAL_REMOTE：只为本地未命中的 key 访问远端，并回填本地 --------------

    @Test
    fun localRemote_asksRemoteOnlyForLocalMisses_andBackfills() {
        val local = RecordingKvManager().apply { put("m1", "k1", "v1") }
        val remote = RecordingKvManager().apply { put("m1", "k2", "v2"); put("m1", "k3", "v3") }
        val mgr = manager(local = local, remote = remote, configs = mapOf("m1" to CacheStrategy.LOCAL_REMOTE))

        val result = mgr.multiGet("m1", listOf("k1", "k2", "k3"))

        assertEquals(
            listOf(listOf<Any>("k2", "k3")), remote.multiGetCalls.map { it.second },
            "本地已命中的 k1 不应再问远端"
        )
        assertEquals(listOf("k1", "k2", "k3"), result.keys.toList(), "结果应保持调用方给定的 key 顺序")
        assertEquals(listOf("v1", "v2", "v3"), result.values.toList())
    }

    @Test
    fun localRemote_allLocalHits_skipsRemoteEntirely() {
        val local = RecordingKvManager().apply { put("m1", "k1", "v1"); put("m1", "k2", "v2") }
        val remote = RecordingKvManager()
        val mgr = manager(local = local, remote = remote, configs = mapOf("m1" to CacheStrategy.LOCAL_REMOTE))

        mgr.multiGet("m1", listOf("k1", "k2"))

        assertTrue(remote.multiGetCalls.isEmpty(), "本地全命中时不应产生任何远端调用")
    }

    // ---- 退化路径：不具备批量能力时必须仍然正确 -------------------------------

    @Test
    fun fallsBackToPerKeyReadsRatherThanReportingEverythingAsMissing() {
        // 本地层是个普通 CacheManager（未实现 IKeyValueCacheManager，因而没有批量能力）。
        // 批量只是优化：拿不到批量能力时必须逐键读，而不是把整批判成未命中——
        // 后者会让 @BatchCacheable 每次都整批回源，比优化前更糟。
        val mgr = managerWithPlainCache("p1")
        mgr.getCache("p1")!!.put("k1", "v1")

        val result = mgr.multiGet("p1", listOf("k1", "k2"))

        assertEquals("v1", result["k1"], "无批量能力时仍应逐键读出已缓存的值")
        assertTrue(!result.containsKey("k2"))
    }

    @Test
    fun emptyKeys_returnsEmptyWithoutTouchingTheCache() {
        val local = RecordingKvManager()
        val mgr = manager(local = local, configs = mapOf("l1" to CacheStrategy.SINGLE_LOCAL))

        assertTrue(mgr.multiGet("l1", emptyList()).isEmpty())
        assertTrue(local.multiGetCalls.isEmpty())
    }

    // ---- helpers -------------------------------------------------------------

    private fun manager(
        local: RecordingKvManager? = null,
        remote: RecordingKvManager? = null,
        configs: Map<String, CacheStrategy>,
    ): MixCacheManager {
        val mgr = MixCacheManager()
        setField(mgr, "isCacheEnabled", true)
        setField(mgr, "versionConfig", CacheVersionConfig().apply { cacheVersion = "v1" })
        setField(mgr, "cacheConfigProvider", StubProvider(configs))
        local?.let { setField(mgr, "localCacheManager", it) }
        remote?.let { setField(mgr, "remoteCacheManager", it) }
        mgr.initCacheAfterSystemInit()
        return mgr
    }

    /**
     * A manager whose local tier is a plain Spring [ConcurrentMapCacheManager] — a `CacheManager` that does
     * **not** implement [IKeyValueCacheManager], so no bulk lookup is available and [MixCacheManager] has to
     * fall back to per-key reads.
     */
    private fun managerWithPlainCache(name: String): MixCacheManager {
        val mgr = MixCacheManager()
        setField(mgr, "isCacheEnabled", true)
        setField(mgr, "versionConfig", CacheVersionConfig().apply { cacheVersion = "v1" })
        setField(mgr, "cacheConfigProvider", StubProvider(mapOf(name to CacheStrategy.SINGLE_LOCAL)))
        setField(mgr, "localCacheManager", ConcurrentMapCacheManager())
        mgr.initCacheAfterSystemInit()
        return mgr
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val f = MixCacheManager::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    /** Records every bulk call so tests can assert that batching actually happened. */
    private open class RecordingKvManager : IKeyValueCacheManager<Cache>, CacheItemInitializing {
        val multiGetCalls = mutableListOf<Pair<String, List<Any>>>()
        protected val caches = mutableMapOf<String, Cache>()
        private val entries = mutableMapOf<String, MutableMap<Any, Any?>>()

        fun put(cacheName: String, key: Any, value: Any?) {
            entries.getOrPut(cacheName) { linkedMapOf() }[key] = value
        }

        override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {}
        override fun createCache(cacheConfig: CacheConfig): Cache =
            ConcurrentMapCache(cacheConfig.name ?: "x", true)
        override fun evictByPattern(cacheName: String, pattern: String) {}
        override fun existsKey(cacheName: String, key: Any): Boolean =
            entries[cacheName]?.containsKey(key) == true

        override fun multiGet(cacheName: String, keys: Collection<Any>): Map<Any, Any?> {
            multiGetCalls.add(cacheName to keys.toList())
            val store = entries[cacheName] ?: return emptyMap()
            val result = LinkedHashMap<Any, Any?>()
            keys.forEach { if (store.containsKey(it)) result[it] = store[it] }
            return result
        }

        override fun getCache(cacheName: String): Cache =
            caches.getOrPut(cacheName) { ConcurrentMapCache(cacheName, true) }

        override fun getCacheNames(): MutableCollection<String> = caches.keys
    }

    private class StubProvider(private val strategies: Map<String, CacheStrategy>) : ICacheConfigProvider {
        private fun of(strategy: CacheStrategy) = strategies.filterValues { it == strategy }
            .mapValues { (name, s) -> CacheConfig().apply { this.name = name; this.strategy = s.name } }

        override fun getCacheConfig(name: String): CacheConfig? = getAllCacheConfigs()[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> =
            of(CacheStrategy.SINGLE_LOCAL) + of(CacheStrategy.REMOTE) + of(CacheStrategy.LOCAL_REMOTE)
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = of(CacheStrategy.SINGLE_LOCAL)
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = of(CacheStrategy.REMOTE)
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = of(CacheStrategy.LOCAL_REMOTE)
    }
}
