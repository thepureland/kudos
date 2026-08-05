package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.CacheItemInitializing
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.support.StaticApplicationContext
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [MixCacheManager]: cache initialization, version-prefixed lookup, strategy
 * up/down-grade, pattern eviction, local clearing, and key-existence checks.
 *
 * All Spring-managed private fields are populated by reflection so no full container is needed.
 *
 * @author K
 * @since 1.0.0
 */
internal class MixCacheManagerTest {

    private lateinit var ctx: StaticApplicationContext
    private lateinit var handler: RecordingHandler

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        handler = RecordingHandler()
        ctx.beanFactory.registerSingleton("recordingHandler", handler)
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        ctx.close()
    }

    // region init: disabled / no-strategy

    @Test
    fun init_cachingDisabled_loadsNothing() {
        val mgr = newManager(enabled = false)
        mgr.initCacheAfterSystemInit()
        assertNull(mgr.getCache("any"))
    }

    @Test
    fun init_noStrategy_loadsNothing() {
        // enabled but neither local nor remote manager injected
        val mgr = newManager(enabled = true, local = null, remote = null,
            provider = StubProvider())
        mgr.initCacheAfterSystemInit()
        assertNull(mgr.getCache("any"))
    }

    @Test
    fun init_enabledButProviderMissing_throws() {
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), provider = null)
        assertFails { mgr.initCacheAfterSystemInit() }
    }

    // endregion

    // region init: load each config type

    @Test
    fun init_loadsLocalRemoteAndMixCaches_andGetCacheUsesVersionPrefix() {
        val local = FakeKvCacheManager()
        val remote = FakeKvCacheManager()
        val provider = StubProvider(
            local = mapOf("l1" to config("l1")),
            remote = mapOf("r1" to config("r1")),
            mix = mapOf("m1" to config("m1"))
        )
        val mgr = newManager(enabled = true, local = local, remote = remote, provider = provider,
            version = "v1")
        mgr.initCacheAfterSystemInit()

        // Local + remote managers received init configs
        assertTrue(local.initialized, "local manager should be initialized")
        assertTrue(remote.initialized, "remote manager should be initialized")

        // Lookup by logical name resolves through the version prefix.
        val l = mgr.getCache("l1") as MixCache
        assertEquals(CacheStrategy.SINGLE_LOCAL, l.strategy)
        val r = mgr.getCache("r1") as MixCache
        assertEquals(CacheStrategy.REMOTE, r.strategy)
        val m = mgr.getCache("m1") as MixCache
        assertEquals(CacheStrategy.LOCAL_REMOTE, m.strategy)
    }

    @Test
    fun init_mixCache_downgradesToLocalWhenRemoteMissing() {
        val local = FakeKvCacheManager()
        val provider = StubProvider(mix = mapOf("m1" to config("m1")))
        val mgr = newManager(enabled = true, local = local, remote = null, provider = provider)
        mgr.initCacheAfterSystemInit()
        val m = mgr.getCache("m1") as MixCache
        assertEquals(CacheStrategy.SINGLE_LOCAL, m.strategy)
    }

    @Test
    fun init_mixCache_upgradesToRemoteWhenLocalMissing() {
        val remote = FakeKvCacheManager()
        val provider = StubProvider(mix = mapOf("m1" to config("m1")))
        val mgr = newManager(enabled = true, local = null, remote = remote, provider = provider)
        mgr.initCacheAfterSystemInit()
        val m = mgr.getCache("m1") as MixCache
        assertEquals(CacheStrategy.REMOTE, m.strategy)
    }

    @Test
    fun init_emptyConfigs_loadsNoCaches() {
        // local manager exists but no config of any type -> warn branches in load* methods
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), remote = FakeKvCacheManager(),
            provider = StubProvider())
        mgr.initCacheAfterSystemInit()
        assertNull(mgr.getCache("nope"))
    }

    // endregion

    // region existsKey

    @Test
    fun existsKey_returnsFalseWhenCacheMissing() {
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), provider = StubProvider())
        mgr.initCacheAfterSystemInit()
        assertFalse(mgr.existsKey("ghost", "k"))
    }

    @Test
    fun existsKey_singleLocal_delegatesToLocalManager() {
        val local = FakeKvCacheManager().apply { existing.add("v1::l1::k") }
        val mgr = newManager(enabled = true, local = local, provider = StubProvider(local = mapOf("l1" to config("l1"))),
            version = "v1")
        mgr.initCacheAfterSystemInit()
        assertTrue(mgr.existsKey("l1", "k"))
        assertFalse(mgr.existsKey("l1", "absent"))
    }

    @Test
    fun existsKey_remote_delegatesToRemoteManager() {
        val remote = FakeKvCacheManager().apply { existing.add("v1::r1::k") }
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), remote = remote,
            provider = StubProvider(remote = mapOf("r1" to config("r1"))), version = "v1")
        mgr.initCacheAfterSystemInit()
        assertTrue(mgr.existsKey("r1", "k"))
    }

    @Test
    fun existsKey_localRemote_trueWhenEitherTierHasIt() {
        val local = FakeKvCacheManager()
        val remote = FakeKvCacheManager().apply { existing.add("v1::m1::k") }
        val mgr = newManager(enabled = true, local = local, remote = remote,
            provider = StubProvider(mix = mapOf("m1" to config("m1"))), version = "v1")
        mgr.initCacheAfterSystemInit()
        assertTrue(mgr.existsKey("m1", "k"), "present in remote tier only -> true")
        assertFalse(mgr.existsKey("m1", "absent"))
    }

    // endregion

    // region evictByPattern

    @Test
    fun evictByPattern_missingCache_isNoOp() {
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), provider = StubProvider())
        mgr.initCacheAfterSystemInit()
        mgr.evictByPattern("ghost", "k") // should not throw
    }

    @Test
    fun evictByPattern_singleLocal_appendsStarAndUsesRealName() {
        val local = FakeKvCacheManager()
        val mgr = newManager(enabled = true, local = local, provider = StubProvider(local = mapOf("l1" to config("l1"))),
            version = "v1")
        mgr.initCacheAfterSystemInit()
        mgr.evictByPattern("l1", "user")
        assertEquals(listOf("v1::l1" to "user*"), local.evictPatternCalls)
    }

    @Test
    fun evictByPattern_remote_usesLogicalName_keepsExistingStar() {
        val remote = FakeKvCacheManager()
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), remote = remote,
            provider = StubProvider(remote = mapOf("r1" to config("r1"))), version = "v1")
        mgr.initCacheAfterSystemInit()
        mgr.evictByPattern("r1", "user:*")
        assertEquals(listOf("r1" to "user:*"), remote.evictPatternCalls)
    }

    @Test
    fun evictByPattern_localRemote_evictsBothAndBroadcasts() {
        val local = FakeKvCacheManager()
        val remote = FakeKvCacheManager()
        val mgr = newManager(enabled = true, local = local, remote = remote,
            provider = StubProvider(mix = mapOf("m1" to config("m1"))), version = "v1")
        mgr.initCacheAfterSystemInit()
        handler.expect(1)

        mgr.evictByPattern("m1", "user")

        assertEquals(listOf("m1" to "user*"), remote.evictPatternCalls)
        assertEquals(listOf("v1::m1" to "user*"), local.evictPatternCalls)
        assertTrue(handler.await(2, TimeUnit.SECONDS), "LOCAL_REMOTE pattern eviction should broadcast")
        assertEquals("user*", handler.messages().single().key)
    }

    // endregion

    // region clearLocal

    @Test
    fun clearLocal_missingCache_isNoOp() {
        val mgr = newManager(enabled = true, local = FakeKvCacheManager(), provider = StubProvider())
        mgr.initCacheAfterSystemInit()
        mgr.clearLocal("ghost", "k") // should not throw
    }

    @Test
    fun clearLocal_patternKey_usesEvictByPattern() {
        val local = FakeKvCacheManager()
        val mgr = newManager(enabled = true, local = local, provider = StubProvider(local = mapOf("l1" to config("l1"))),
            version = "v1")
        mgr.initCacheAfterSystemInit()
        mgr.clearLocal("l1", "user:*")
        assertEquals(listOf("v1::l1" to "user:*"), local.evictPatternCalls)
    }

    @Test
    fun clearLocal_singleKey_clearsLocalTier() {
        val local = ConcurrentMapBackedKvManager()
        val mgr = newManager(enabled = true, local = local, provider = StubProvider(local = mapOf("l1" to config("l1"))),
            version = "v1")
        mgr.initCacheAfterSystemInit()
        // seed local tier
        local.getCache("v1::l1").put("k", "v")
        mgr.clearLocal("l1", "k")
        assertNull(local.getCache("v1::l1").get("k"))
    }

    @Test
    fun clearLocal_acceptsAlreadyPrefixedName() {
        val local = ConcurrentMapBackedKvManager()
        val mgr = newManager(enabled = true, local = local, provider = StubProvider(local = mapOf("l1" to config("l1"))),
            version = "v1")
        mgr.initCacheAfterSystemInit()
        local.getCache("v1::l1").put("k", "v")
        // pass the already-prefixed name; getRealCacheName strips it, getFinalCacheName re-adds -> same cache
        mgr.clearLocal("v1::l1", "k")
        assertNull(local.getCache("v1::l1").get("k"))
    }

    @Test
    fun getCache_versionConfigMissing_throws() {
        val mgr = MixCacheManager()
        setField(mgr, "isCacheEnabled", true)
        // versionConfig stays null -> requireVersionConfig throws
        assertFails { mgr.getCache("x") }
    }

    // endregion

    // ----- helpers --------------------------------------------------------

    private fun config(name: String) = CacheConfig().apply { this.name = name; active = true }

    private fun newManager(
        enabled: Boolean?,
        local: CacheManager? = null,
        remote: CacheManager? = null,
        provider: ICacheConfigProvider? = null,
        version: String = "v1",
    ): MixCacheManager {
        val mgr = MixCacheManager()
        setField(mgr, "isCacheEnabled", enabled)
        setField(mgr, "versionConfig", CacheVersionConfig().apply { cacheVersion = version })
        if (local != null) setField(mgr, "localCacheManager", local)
        setField(mgr, "remoteCacheManager", remote)
        setField(mgr, "cacheConfigProvider", provider)
        return mgr
    }

    private fun setField(target: Any, name: String, value: Any?) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val f = clazz.getDeclaredField(name)
                f.isAccessible = true
                f.set(target, value)
                return
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        error("field not found: $name")
    }

    /** Stub config provider returning the three config groups. */
    private class StubProvider(
        private val local: Map<String, CacheConfig> = emptyMap(),
        private val remote: Map<String, CacheConfig> = emptyMap(),
        private val mix: Map<String, CacheConfig> = emptyMap(),
    ) : ICacheConfigProvider {
        override fun getCacheConfig(name: String): CacheConfig? =
            local[name] ?: remote[name] ?: mix[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = local + remote + mix
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = local
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = remote
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = mix
    }

    /** Fake key-value cache manager implementing the kit interfaces for delegation assertions. */
    private open class FakeKvCacheManager : IKeyValueCacheManager<Cache>, CacheItemInitializing {
        var initialized = false
        val existing = mutableSetOf<String>() // "<realName>::<key>"
        val evictPatternCalls = mutableListOf<Pair<String, String>>()
        private val caches = mutableMapOf<String, Cache>()

        override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
            initialized = true
        }
        override fun createCache(cacheConfig: CacheConfig): Cache =
            ConcurrentMapCache(cacheConfig.name ?: "x", true)
        override fun evictByPattern(cacheName: String, pattern: String) {
            evictPatternCalls.add(cacheName to pattern)
        }
        override fun existsKey(cacheName: String, key: Any): Boolean =
            existing.contains("$cacheName::$key")
        override fun getCache(name: String): Cache = caches.getOrPut(name) { ConcurrentMapCache(name, true) }
        override fun getCacheNames(): MutableCollection<String> = caches.keys
    }

    /** A manager that returns real ConcurrentMapCache instances so clearLocal single-key path is observable. */
    private class ConcurrentMapBackedKvManager : FakeKvCacheManager()

    private class RecordingHandler : ICacheMessageHandler {
        private val received = Collections.synchronizedList(mutableListOf<CacheMessage>())
        @Volatile private var latch = CountDownLatch(0)
        fun expect(count: Int) { received.clear(); latch = CountDownLatch(count) }
        fun await(timeout: Long, unit: TimeUnit) = latch.await(timeout, unit)
        fun messages(): List<CacheMessage> = received.toList()
        override fun sendMessage(message: CacheMessage) { received.add(message); latch.countDown() }
        override fun receiveMessage(message: CacheMessage) {}
    }
}
