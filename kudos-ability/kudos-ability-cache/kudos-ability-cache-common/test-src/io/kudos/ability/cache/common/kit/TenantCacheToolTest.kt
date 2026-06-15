package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.mockito.Mockito
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
 * Unit tests for [TenantCacheTool].
 *
 * The tool prepends a `"{tenantId}::"` prefix to every key and delegates to [KeyValueCacheKit]. Tests assert
 * the tenant-prefixed key is what actually reaches the underlying cache, plus the active guards on the
 * pattern-based clear operations. KeyValueCacheKit is stubbed via overrideForTesting + an in-memory cache.
 *
 * Coverage:
 *  - getTenantKey behavior via put/getValue (tenant prefix, Unicode key, null tenant -> "null::").
 *  - putIfAbsent / doEvict apply the tenant prefix.
 *  - isCacheActive / isWriteInTime / getCache / getCacheConfig pass-through.
 *  - clear / doClear / evictByPattern inactive guards (no-op) and active null-manager early return.
 *  - clearAllTenants delegates to KeyValueCacheKit.clear (cross-tenant).
 *
 * @author K
 * @since 1.0.0
 */
internal class TenantCacheToolTest {

    private lateinit var mgr: TestMixCacheManager
    private lateinit var provider: TestProvider
    private var ctx: AnnotationConfigApplicationContext? = null

    @BeforeTest
    fun setup() {
        mgr = TestMixCacheManager()
        provider = TestProvider()
        KeyValueCacheKit.overrideForTesting(mgr, provider)
        ctx = AnnotationConfigApplicationContext(EmptyConfig::class.java)
        SpringKit.applicationContext = ctx
        KudosContextHolder.get().tenantId = "T1"
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
        KudosContextHolder.clear()
        ctx?.close()
        SpringKit.applicationContext = null
    }

    // ---- tenant-key prefixing ----------------------------------------------

    @Test
    fun put_thenGetValue_useTenantPrefixedKey() {
        provider.register("C", true, CacheStrategy.REMOTE)
        TenantCacheTool.put("C", "user:1", "v")
        // The underlying cache must hold the prefixed key.
        assertEquals("v", mgr.underlying("C").get("T1::user:1")?.get())
        // And read-back through the tool resolves the same prefix.
        assertEquals("v", TenantCacheTool.getValue("C", "user:1"))
    }

    @Test
    fun getValueTyped_useTenantPrefixedKey() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("T1::k", "v")
        assertEquals("v", TenantCacheTool.getValue("C", "k", String::class))
    }

    @Test
    fun unicodeKey_isPrefixed() {
        provider.register("C", true, CacheStrategy.REMOTE)
        TenantCacheTool.put("C", "用户:1", "值")
        assertEquals("值", mgr.underlying("C").get("T1::用户:1")?.get())
    }

    @Test
    fun nullTenant_keyPrefixedWithLiteralNull() {
        KudosContextHolder.get().tenantId = null
        provider.register("C", true, CacheStrategy.REMOTE)
        TenantCacheTool.put("C", "k", "v")
        assertEquals("v", mgr.underlying("C").get("null::k")?.get())
    }

    @Test
    fun putIfAbsent_usesTenantPrefix_keepsExisting() {
        provider.register("C", true, CacheStrategy.REMOTE)
        TenantCacheTool.putIfAbsent("C", "k", "first")
        TenantCacheTool.putIfAbsent("C", "k", "second")
        assertEquals("first", mgr.underlying("C").get("T1::k")?.get())
    }

    @Test
    fun doEvict_usesTenantPrefix() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("T1::k", "v")
        TenantCacheTool.doEvict("C", "k")
        assertNull(mgr.underlying("C").get("T1::k"))
    }

    @Test
    fun evict_usesTenantPrefix() {
        provider.register("C", true, CacheStrategy.REMOTE)
        mgr.underlying("C").put("T1::k", "v")
        TenantCacheTool.evict("C", "k")
        assertNull(mgr.underlying("C").get("T1::k"))
    }

    // ---- pass-through queries ----------------------------------------------

    @Test
    fun isCacheActive_passThrough() {
        provider.register("C", true, CacheStrategy.REMOTE)
        assertTrue(TenantCacheTool.isCacheActive("C"))
        provider.register("D", false, CacheStrategy.REMOTE)
        assertFalse(TenantCacheTool.isCacheActive("D"))
    }

    @Test
    fun getCache_passThrough() {
        provider.register("C", true, CacheStrategy.REMOTE)
        assertTrue(TenantCacheTool.getCache("C") is MixCache)
    }

    @Test
    fun isWriteInTime_passThrough() {
        provider.register("C", true, CacheStrategy.REMOTE, writeInTime = true)
        assertTrue(TenantCacheTool.isWriteInTime("C"))
    }

    @Test
    fun getCacheConfig_passThrough() {
        provider.register("C", true, CacheStrategy.REMOTE)
        assertEquals("C", TenantCacheTool.getCacheConfig("C")?.name)
    }

    // ---- clear / pattern guards --------------------------------------------

    @Test
    fun clear_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        TenantCacheTool.clear("C") // returns at active guard, no throw
    }

    @Test
    fun doClear_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        TenantCacheTool.doClear("C")
    }

    @Test
    fun evictByPattern_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        TenantCacheTool.evictByPattern("C", "x")
    }

    @Test
    fun clear_active_nullManager_earlyReturn() {
        provider.register("C", true, CacheStrategy.REMOTE)
        KeyValueCacheKit.overrideForTesting(null, provider)
        // evictByPattern hits `getCacheManager() ?: return` -> no throw.
        TenantCacheTool.clear("C")
    }

    @Test
    fun clear_active_evictsCurrentTenantPattern() {
        provider.register("C", true, CacheStrategy.REMOTE)
        val mockMgr = Mockito.mock(MixCacheManager::class.java)
        KeyValueCacheKit.overrideForTesting(mockMgr, provider)
        TenantCacheTool.clear("C")
        // clear -> evictByPattern with "{tenantId}::*"; KeyValueCacheKit.evictByPattern delegates to the manager.
        Mockito.verify(mockMgr).evictByPattern("C", "T1::*")
    }

    @Test
    fun doClear_active_evictsCurrentTenantPattern() {
        provider.register("C", true, CacheStrategy.REMOTE)
        val mockMgr = Mockito.mock(MixCacheManager::class.java)
        KeyValueCacheKit.overrideForTesting(mockMgr, provider)
        TenantCacheTool.doClear("C")
        Mockito.verify(mockMgr).evictByPattern("C", "T1::*")
    }

    @Test
    fun evictByPattern_active_appliesTenantPrefix() {
        provider.register("C", true, CacheStrategy.REMOTE)
        val mockMgr = Mockito.mock(MixCacheManager::class.java)
        KeyValueCacheKit.overrideForTesting(mockMgr, provider)
        TenantCacheTool.evictByPattern("C", "user:*")
        Mockito.verify(mockMgr).evictByPattern("C", "T1::user:*")
    }

    @Test
    fun reload_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        TenantCacheTool.reload("C", "k")
    }

    @Test
    fun reloadAll_inactive_isNoOp() {
        provider.register("C", false, CacheStrategy.REMOTE)
        TenantCacheTool.reloadAll("C")
    }

    @Test
    fun clearAllTenants_active_nullManager_earlyReturn() {
        provider.register("C", true, CacheStrategy.REMOTE)
        KeyValueCacheKit.overrideForTesting(null, provider)
        // clearAllTenants -> KeyValueCacheKit.clear -> getCache(null manager) -> null -> return, no throw.
        TenantCacheTool.clearAllTenants("C")
    }

    // ---- test doubles -------------------------------------------------------

    @Configuration
    open class EmptyConfig

    private class TestMixCacheManager : MixCacheManager() {
        private val under = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        fun underlying(name: String): Cache = under.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache =
            wrappers.computeIfAbsent(name) { MixCache(CacheStrategy.REMOTE, null, underlying(it)) }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean, strategy: CacheStrategy, writeInTime: Boolean = false) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name; this.writeInTime = writeInTime
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
