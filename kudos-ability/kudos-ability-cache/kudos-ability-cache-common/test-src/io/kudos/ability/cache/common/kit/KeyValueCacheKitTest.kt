package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [KeyValueCacheKit].
 *
 * Uses [KeyValueCacheKit.overrideForTesting] to inject an in-memory MixCacheManager + config provider so no
 * real cache infrastructure is required. The SINGLE_LOCAL evict/clear branches route through
 * `CacheOperatorVo.doNotify()`; an empty Spring context (no NotifyTool) is wired so doNotify degrades to the
 * local-cleanup fallback, which is the behavior asserted here.
 *
 * Coverage:
 *  - isCacheActive: active / inactive / unknown.
 *  - getCache: present / null-manager / missing cache.
 *  - getValue (typed + untyped), put / putIfAbsent (active + inactive guard).
 *  - evict: REMOTE -> direct; SINGLE_LOCAL -> notify (degraded to local); non-MixCache -> no-op.
 *  - doEvict / clear / doClear with the active guard.
 *  - isWriteInTime, getCacheConfig (active guard + unknown), existsKey/evictByPattern inactive guards.
 *  - reload / reloadAll writeOnBoot=false branches (route to evict / clear).
 *
 * @author K
 * @since 1.0.0
 */
internal class KeyValueCacheKitTest {

    private lateinit var mgr: TestMixCacheManager
    private lateinit var provider: TestProvider
    private var ctx: AnnotationConfigApplicationContext? = null

    @BeforeTest
    fun setup() {
        mgr = TestMixCacheManager()
        provider = TestProvider()
        KeyValueCacheKit.overrideForTesting(mgr, provider)
        // Empty Spring context so CacheOperatorVo.doNotify() finds no NotifyTool and degrades to local cleanup.
        ctx = AnnotationConfigApplicationContext(EmptyConfig::class.java)
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
        ctx?.close()
        SpringKit.applicationContext = null
    }

    // ---- isCacheActive ------------------------------------------------------

    @Test
    fun isCacheActive_true_whenActive() {
        provider.register("C", active = true, strategy = CacheStrategy.REMOTE)
        assertTrue(KeyValueCacheKit.isCacheActive("C"))
    }

    @Test
    fun isCacheActive_false_whenInactive() {
        provider.register("C", active = false, strategy = CacheStrategy.REMOTE)
        assertFalse(KeyValueCacheKit.isCacheActive("C"))
    }

    @Test
    fun isCacheActive_false_whenUnknown() {
        assertFalse(KeyValueCacheKit.isCacheActive("nope"))
    }

    // ---- getCache -----------------------------------------------------------

    @Test
    fun getCache_returnsCache() {
        assertTrue(KeyValueCacheKit.getCache("C") is MixCache)
    }

    @Test
    fun getCache_nullManager_returnsNull() {
        KeyValueCacheKit.overrideForTesting(null, provider)
        // No mixCacheManager bean in the empty context -> getCacheManager() returns null.
        assertNull(KeyValueCacheKit.getCache("C"))
    }

    @Test
    fun getCache_missingCache_returnsNull() {
        val emptyMgr = object : MixCacheManager() {
            override fun getCache(name: String): Cache? = null
        }
        KeyValueCacheKit.overrideForTesting(emptyMgr, provider)
        assertNull(KeyValueCacheKit.getCache("absent"))
    }

    // ---- getValue / put / putIfAbsent --------------------------------------

    @Test
    fun getValue_typed_returnsValue() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("k", "v")
        assertEquals("v", KeyValueCacheKit.getValue("C", "k", String::class))
    }

    @Test
    fun getValue_typed_nullCache_returnsNull() {
        val emptyMgr = object : MixCacheManager() { override fun getCache(name: String): Cache? = null }
        KeyValueCacheKit.overrideForTesting(emptyMgr, provider)
        assertNull(KeyValueCacheKit.getValue("C", "k", String::class))
    }

    @Test
    fun getValue_untyped_returnsValue() {
        mgr.underlying("C").put("k", 123)
        assertEquals(123, KeyValueCacheKit.getValue("C", "k"))
    }

    @Test
    fun put_active_writesValue() {
        provider.register("C", true, CacheStrategy.REMOTE)
        KeyValueCacheKit.put("C", "k", "v")
        assertEquals("v", mgr.underlying("C").get("k")?.get())
    }

    @Test
    fun put_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        KeyValueCacheKit.put("C", "k", "v")
        assertNull(mgr.underlying("C").get("k"))
    }

    @Test
    fun putIfAbsent_active_writesWhenAbsent_keepsExisting() {
        provider.register("C", true, CacheStrategy.REMOTE)
        KeyValueCacheKit.putIfAbsent("C", "k", "first")
        KeyValueCacheKit.putIfAbsent("C", "k", "second")
        assertEquals("first", mgr.underlying("C").get("k")?.get())
    }

    @Test
    fun putIfAbsent_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        KeyValueCacheKit.putIfAbsent("C", "k", "v")
        assertNull(mgr.underlying("C").get("k"))
    }

    // ---- evict / doEvict ----------------------------------------------------

    @Test
    fun evict_remote_directEviction() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.evict("C", "k")
        assertNull(mgr.underlying("C").get("k"))
    }

    @Test
    fun evict_singleLocal_degradesToLocalEviction() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL)
        mgr.strategyFor["C"] = CacheStrategy.SINGLE_LOCAL
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.evict("C", "k")
        // doNotify finds no NotifyTool -> local fallback evicts the key.
        assertNull(mgr.underlying("C").get("k"))
    }

    @Test
    fun evict_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.evict("C", "k")
        assertEquals("v", mgr.underlying("C").get("k")?.get())
    }

    @Test
    fun evict_nonMixCache_isNoOp() {
        // Manager returns a plain (non-MixCache) cache -> `as? MixCache` is null -> early return, no throw.
        val plainMgr = object : MixCacheManager() {
            private val c = ConcurrentMapCache("C", true)
            override fun getCache(name: String): Cache = c
        }
        provider.register("C", true, CacheStrategy.REMOTE)
        KeyValueCacheKit.overrideForTesting(plainMgr, provider)
        KeyValueCacheKit.evict("C", "k") // must not throw
    }

    @Test
    fun doEvict_removesKey() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.doEvict("C", "k")
        assertNull(mgr.underlying("C").get("k"))
    }

    @Test
    fun doEvict_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.doEvict("C", "k")
        assertEquals("v", mgr.underlying("C").get("k")?.get())
    }

    // ---- clear / doClear ----------------------------------------------------

    @Test
    fun clear_remote_directClear() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.clear("C")
        assertNull(mgr.underlying("C").get("a"))
    }

    @Test
    fun clear_singleLocal_degradesToLocalClear() {
        provider.register("C", true, CacheStrategy.SINGLE_LOCAL)
        mgr.strategyFor["C"] = CacheStrategy.SINGLE_LOCAL
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.clear("C")
        assertNull(mgr.underlying("C").get("a"))
    }

    @Test
    fun clear_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.clear("C")
        assertEquals("1", mgr.underlying("C").get("a")?.get())
    }

    @Test
    fun doClear_clearsCache() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.doClear("C")
        assertNull(mgr.underlying("C").get("a"))
    }

    @Test
    fun doClear_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.doClear("C")
        assertEquals("1", mgr.underlying("C").get("a")?.get())
    }

    // ---- isWriteInTime / getCacheConfig ------------------------------------

    @Test
    fun isWriteInTime_trueWhenConfigured() {
        provider.register("C", true, CacheStrategy.REMOTE, writeInTime = true)
        assertTrue(KeyValueCacheKit.isWriteInTime("C"))
    }

    @Test
    fun isWriteInTime_falseWhenNotConfigured() {
        provider.register("C", true, CacheStrategy.REMOTE, writeInTime = false)
        assertFalse(KeyValueCacheKit.isWriteInTime("C"))
    }

    @Test
    fun isWriteInTime_falseWhenInactive() {
        provider.register("C", false, CacheStrategy.REMOTE, writeInTime = true)
        assertFalse(KeyValueCacheKit.isWriteInTime("C"))
    }

    @Test
    fun getCacheConfig_returnsConfig() {
        provider.register("C", true, CacheStrategy.REMOTE)
        assertEquals("C", KeyValueCacheKit.getCacheConfig("C")?.name)
    }

    @Test
    fun getCacheConfig_inactive_returnsNull() {
        provider.register("C", false, CacheStrategy.REMOTE)
        assertNull(KeyValueCacheKit.getCacheConfig("C"))
    }

    // ---- reload / reloadAll (writeOnBoot=false branches) -------------------

    @Test
    fun reload_writeOnBootFalse_evicts() {
        provider.register("C", true, CacheStrategy.REMOTE, writeOnBoot = false)
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.reload("C", "k")
        assertNull(mgr.underlying("C").get("k"), "writeOnBoot=false reload should fall through to evict")
    }

    @Test
    fun reload_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        mgr.underlying("C").put("k", "v")
        KeyValueCacheKit.reload("C", "k")
        assertEquals("v", mgr.underlying("C").get("k")?.get())
    }

    @Test
    fun reloadAll_writeOnBootFalse_clears() {
        provider.register("C", true, CacheStrategy.REMOTE, writeOnBoot = false)
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.reloadAll("C")
        assertNull(mgr.underlying("C").get("a"), "writeOnBoot=false reloadAll should fall through to clear")
    }

    @Test
    fun reloadAll_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        mgr.underlying("C").put("a", "1")
        KeyValueCacheKit.reloadAll("C")
        assertEquals("1", mgr.underlying("C").get("a")?.get())
    }

    // ---- existsKey / evictByPattern inactive guards ------------------------

    @Test
    fun existsKey_inactive_false() {
        provider.register("C", false, CacheStrategy.REMOTE)
        assertFalse(KeyValueCacheKit.existsKey("C", "k"))
    }

    @Test
    fun evictByPattern_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        // Inactive guard returns before touching the manager; must not throw.
        KeyValueCacheKit.evictByPattern("C", "x*")
    }

    @Test
    fun evictByPattern_active_nullManager_isNoOp() {
        provider.register("C", true, CacheStrategy.REMOTE)
        // Override with a null manager so getCacheManager() returns null and the method returns early
        // (the real evictByPattern needs Redis/Caffeine managers, covered by integration tests).
        KeyValueCacheKit.overrideForTesting(null, provider)
        KeyValueCacheKit.evictByPattern("C", "x*") // returns at `getCacheManager() ?: return`
    }

    // ---- test doubles -------------------------------------------------------

    @Configuration
    open class EmptyConfig

    private class TestMixCacheManager : MixCacheManager() {
        private val under = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        val strategyFor = ConcurrentHashMap<String, CacheStrategy>()
        fun underlying(name: String): Cache = under.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache {
            val strategy = strategyFor[name] ?: CacheStrategy.REMOTE
            return wrappers.compute(name) { n, existing ->
                if (existing != null && existing.strategy == strategy) existing
                else MixCache(strategy, if (strategy == CacheStrategy.REMOTE) null else underlying(n),
                    if (strategy == CacheStrategy.REMOTE) underlying(n) else null)
            }!!
        }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(
            name: String,
            active: Boolean,
            strategy: CacheStrategy,
            writeInTime: Boolean = false,
            writeOnBoot: Boolean = false,
        ) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name
                this.writeInTime = writeInTime; this.writeOnBoot = writeOnBoot
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
