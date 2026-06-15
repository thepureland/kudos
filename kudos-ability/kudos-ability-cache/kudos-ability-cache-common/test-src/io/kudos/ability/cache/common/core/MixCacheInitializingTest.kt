package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MixCacheInitializing.afterSingletonsInstantiated]:
 *  - not-injected cacheManager -> logs error and returns without calling init,
 *  - injected cacheManager (+ optional hashCacheManager) -> both init methods run (observed via the
 *    config provider being queried).
 *
 * Because `MixCacheManager.initCacheAfterSystemInit` / `MixHashCacheManager.initHashCacheAfterSystemInit`
 * are not mockable here, the "was called" assertion is made through a counting [ICacheConfigProvider]:
 * both init methods query the provider as their first real step.
 *
 * @author K
 * @since 1.0.0
 */
internal class MixCacheInitializingTest {

    @Test
    fun cacheManagerNotInjected_returnsWithoutInit() {
        val initializing = MixCacheInitializing()
        // Do not set the lateinit cacheManager -> isInitialized == false branch.
        initializing.afterSingletonsInstantiated() // must not throw NPE
    }

    @Test
    fun cacheManagerInjected_callsKvInit_andHashInitWhenPresent() {
        val kvProvider = CountingProvider()
        val hashProvider = CountingProvider()

        val kv = buildKvManager(kvProvider)
        val hash = buildHashManager(hashProvider)

        val initializing = MixCacheInitializing()
        setField(initializing, "cacheManager", kv)
        setField(initializing, "hashCacheManager", hash)

        initializing.afterSingletonsInstantiated()

        assertTrue(kvProvider.localQueries.get() > 0, "KV init should have queried the config provider")
        assertTrue(hashProvider.hashQueries.get() > 0, "Hash init should have queried the config provider")
    }

    @Test
    fun hashManagerNull_onlyKvInitRuns() {
        val kvProvider = CountingProvider()
        val kv = buildKvManager(kvProvider)

        val initializing = MixCacheInitializing()
        setField(initializing, "cacheManager", kv)
        // hashCacheManager stays null

        initializing.afterSingletonsInstantiated()

        assertTrue(kvProvider.localQueries.get() > 0)
    }

    // ---- helpers ----

    private fun buildKvManager(provider: ICacheConfigProvider): MixCacheManager {
        val mgr = MixCacheManager()
        setPrivate(mgr, MixCacheManager::class.java, "isCacheEnabled", true)
        setPrivate(mgr, MixCacheManager::class.java, "versionConfig", CacheVersionConfig().apply { cacheVersion = "v" })
        setPrivate(mgr, MixCacheManager::class.java, "cacheConfigProvider", provider)
        // localCacheManager / remoteCacheManager left unset (null/uninitialized) -> init proceeds but the
        // "no cache strategy" branch returns AFTER nothing was queried; so provide a local manager.
        setPrivate(mgr, MixCacheManager::class.java, "localCacheManager", org.springframework.cache.support.NoOpCacheManager())
        return mgr
    }

    private fun buildHashManager(provider: ICacheConfigProvider): MixHashCacheManager {
        val mgr = MixHashCacheManager()
        // isCacheEnabled defaults to true (the val field). Set provider so getHashCacheConfigs() is queried.
        setPrivate(mgr, MixHashCacheManager::class.java, "cacheConfigProvider", provider)
        return mgr
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val f = target.javaClass.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    private fun setPrivate(target: Any, clazz: Class<*>, name: String, value: Any?) {
        val f = clazz.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    private class CountingProvider : ICacheConfigProvider {
        val localQueries = AtomicInteger(0)
        val hashQueries = AtomicInteger(0)
        override fun getCacheConfig(name: String): CacheConfig? = null
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> {
            localQueries.incrementAndGet(); return emptyMap()
        }
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getHashCacheConfigs(): Map<String, CacheConfig> {
            hashQueries.incrementAndGet(); return emptyMap()
        }
    }
}
