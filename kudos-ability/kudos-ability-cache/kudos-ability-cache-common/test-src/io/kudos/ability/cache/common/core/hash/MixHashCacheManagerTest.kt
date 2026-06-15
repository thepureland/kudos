package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.aop.hash.FakeHashCache
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [MixHashCacheManager.initHashCacheAfterSystemInit] / [MixHashCacheManager.getHashCache]:
 * short-circuit branches (disabled / no provider / empty configs / no impl / null version) and the
 * strategy resolution (default REMOTE, explicit values) with version-prefixed lookup.
 *
 * @author K
 * @since 1.0.0
 */
internal class MixHashCacheManagerTest {

    @Test
    fun init_disabled_loadsNothing() {
        val mgr = build(enabled = false, provider = provider("h1" to config("h1")))
        mgr.initHashCacheAfterSystemInit()
        assertNull(mgr.getHashCache("h1"))
    }

    @Test
    fun init_noProvider_loadsNothing() {
        val mgr = build(enabled = true, provider = null)
        mgr.initHashCacheAfterSystemInit()
        assertNull(mgr.getHashCache("h1"))
    }

    @Test
    fun init_emptyConfigs_loadsNothing() {
        val mgr = build(enabled = true, provider = provider())
        mgr.initHashCacheAfterSystemInit()
        assertNull(mgr.getHashCache("h1"))
    }

    @Test
    fun init_noLocalNoRemoteImpl_loadsNothing() {
        val mgr = build(enabled = true, provider = provider("h1" to config("h1")), local = null, remote = null)
        mgr.initHashCacheAfterSystemInit()
        assertNull(mgr.getHashCache("h1"))
    }

    @Test
    fun init_versionConfigNull_loadsNothing() {
        val mgr = build(enabled = true, provider = provider("h1" to config("h1")),
            local = FakeHashCache(), version = null)
        mgr.initHashCacheAfterSystemInit()
        assertNull(mgr.getHashCache("h1"))
    }

    @Test
    fun init_defaultStrategyIsRemote() {
        val cfg = config("h1") // no strategy -> REMOTE
        val mgr = build(enabled = true, provider = provider("h1" to cfg),
            local = FakeHashCache(), remote = FakeHashCache(), version = "v1")
        mgr.initHashCacheAfterSystemInit()
        val cache = mgr.getHashCache("h1")
        assertNotNull(cache)
        assertEquals(CacheStrategy.REMOTE, strategyOf(cache))
    }

    @Test
    fun init_explicitStrategyHonored_andVersionPrefixApplied() {
        val cfg = config("h1").apply { strategy = "LOCAL_REMOTE" }
        val mgr = build(enabled = true, provider = provider("h1" to cfg),
            local = FakeHashCache(), remote = FakeHashCache(), version = "v9")
        mgr.initHashCacheAfterSystemInit()
        val cache = mgr.getHashCache("h1")
        assertNotNull(cache)
        assertEquals(CacheStrategy.LOCAL_REMOTE, strategyOf(cache))
    }

    @Test
    fun getHashCache_unknownName_returnsNull() {
        val mgr = build(enabled = true, provider = provider("h1" to config("h1")),
            local = FakeHashCache(), version = "v1")
        mgr.initHashCacheAfterSystemInit()
        assertNull(mgr.getHashCache("nope"))
    }

    @Test
    fun getHashCache_nullVersionConfig_usesRawName() {
        // version null path of getHashCache: realName falls back to cacheName.
        val mgr = MixHashCacheManager()
        setField(mgr, "isCacheEnabled", true)
        // no versionConfig set -> getHashCache uses raw name; map empty -> null
        assertNull(mgr.getHashCache("anything"))
    }

    // ---- helpers ----

    private fun config(name: String) = CacheConfig().apply { this.name = name; active = true; hash = true }

    private fun provider(vararg entries: Pair<String, CacheConfig>): ICacheConfigProvider =
        object : ICacheConfigProvider {
            private val map = entries.toMap()
            override fun getCacheConfig(name: String): CacheConfig? = map[name]
            override fun getAllCacheConfigs(): Map<String, CacheConfig> = map
            override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
            override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
            override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
            override fun getHashCacheConfigs(): Map<String, CacheConfig> = map
        }

    private fun build(
        enabled: Boolean,
        provider: ICacheConfigProvider?,
        local: IHashCache? = null,
        remote: IHashCache? = null,
        version: String? = "v1",
    ): MixHashCacheManager {
        val mgr = MixHashCacheManager()
        setField(mgr, "isCacheEnabled", enabled)
        setField(mgr, "cacheConfigProvider", provider)
        setField(mgr, "localHashCache", local)
        setField(mgr, "remoteHashCache", remote)
        setField(mgr, "versionConfig", version?.let { CacheVersionConfig().apply { cacheVersion = it } })
        return mgr
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val f = MixHashCacheManager::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    /** Reads the strategy of the wrapped MixHashCache via reflection (it is internal). */
    private fun strategyOf(cache: IHashCache): CacheStrategy {
        val f = cache.javaClass.getDeclaredField("strategy")
        f.isAccessible = true
        return f.get(cache) as CacheStrategy
    }
}
