package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for [CacheDataInitializer]: handler collection during bean post-processing and the
 * boot-time reload that only fires for caches configured with `writeOnBoot = true`.
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheDataInitializerTest {

    private lateinit var provider: TestProvider

    @BeforeTest
    fun setup() {
        provider = TestProvider()
        KeyValueCacheKit.overrideForTesting(configProvider = provider)
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
    }

    @Test
    fun postProcess_returnsSameBean() {
        val initializer = CacheDataInitializer()
        val bean = Any()
        assertSame(bean, initializer.postProcessAfterInitialization(bean, "x"))
    }

    @Test
    fun postProcess_ignoresNonHandlerBeans() {
        val initializer = CacheDataInitializer()
        initializer.postProcessAfterInitialization("not a handler", "s")
        // afterSingletonsInstantiated must not throw / reload anything
        initializer.loadBootCaches()
    }

    @Test
    fun reload_onlyForWriteOnBootCaches() {
        provider.register("boot", active = true, writeOnBoot = true)
        provider.register("noboot", active = true, writeOnBoot = false)
        provider.register("missingConfig-handler", active = false, writeOnBoot = true) // inactive -> getCacheConfig null

        val bootHandler = RecordingHandler("boot")
        val noBootHandler = RecordingHandler("noboot")
        val noConfigHandler = RecordingHandler("missingConfig-handler")

        val initializer = CacheDataInitializer()
        initializer.postProcessAfterInitialization(bootHandler, "b")
        initializer.postProcessAfterInitialization(noBootHandler, "n")
        initializer.postProcessAfterInitialization(noConfigHandler, "m")

        initializer.loadBootCaches()

        assertEquals(listOf(false), bootHandler.reloadCalls, "writeOnBoot=true -> reloadAll(false)")
        assertEquals(emptyList(), noBootHandler.reloadCalls, "writeOnBoot=false -> no reload")
        assertEquals(emptyList(), noConfigHandler.reloadCalls, "inactive cache -> config null -> no reload")
    }

    private class RecordingHandler(private val name: String) : AbstractCacheHandler<String>() {
        val reloadCalls = mutableListOf<Boolean>()
        override fun cacheName(): String = name
        override fun reloadAll(clear: Boolean) { reloadCalls.add(clear) }
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean, writeOnBoot: Boolean) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.writeOnBoot = writeOnBoot
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
