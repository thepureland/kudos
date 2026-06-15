package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.kit.SpringKit
import org.mockito.Mockito
import org.springframework.context.support.GenericApplicationContext
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Spring-backed tests for [KeyValueCacheKit] that exercise the production lookup paths NOT reachable through
 * overrideForTesting:
 *  - getCacheManager() / getCacheConfigProvider() resolving via SpringKit beans.
 *  - handlersFor() scanning AbstractKeyValueCacheHandler beans (lazy double-checked index).
 *  - reload / reloadAll with writeOnBoot=true delegating to the handler.
 *  - evictByPattern / existsKey delegating to a (mocked) MixCacheManager.
 *
 * No overrideForTesting is used here: dependencies come from a real ApplicationContext registered into
 * [SpringKit]. The MixCacheManager is a Mockito mock (its evictByPattern / existsKey are final).
 *
 * @author K
 * @since 1.0.0
 */
internal class KeyValueCacheKitSpringTest {

    private lateinit var ctx: GenericApplicationContext
    private lateinit var mockMgr: MixCacheManager
    private lateinit var provider: TestProvider
    private lateinit var handler: RecordingHandler

    @BeforeTest
    fun setup() {
        // Ensure no stale overrides / handler index from other tests in the same JVM.
        KeyValueCacheKit.resetForTesting()

        mockMgr = Mockito.mock(MixCacheManager::class.java)
        Mockito.`when`(mockMgr.getCache(Mockito.anyString())).thenReturn(null)

        provider = TestProvider()
        handler = RecordingHandler("C")

        ctx = GenericApplicationContext(DefaultListableBeanFactory())
        ctx.beanFactory.registerSingleton("mixCacheManager", mockMgr)
        ctx.beanFactory.registerSingleton("cacheConfigProvider", provider)
        ctx.beanFactory.registerSingleton("recordingHandler", handler)
        ctx.refresh()
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
        ctx.close()
        SpringKit.applicationContext = null
    }

    @Test
    fun isCacheActive_resolvesProviderFromSpring() {
        provider.register("C", true, CacheStrategy.REMOTE)
        assertTrue(KeyValueCacheKit.isCacheActive("C"))
    }

    @Test
    fun reload_writeOnBootTrue_callsHandlerReload() {
        provider.register("C", true, CacheStrategy.REMOTE, writeOnBoot = true)
        KeyValueCacheKit.reload("C", "k")
        assertEquals(listOf("k"), handler.reloadKeys)
    }

    @Test
    fun reloadAll_writeOnBootTrue_callsHandlerReloadAll() {
        provider.register("C", true, CacheStrategy.REMOTE, writeOnBoot = true)
        KeyValueCacheKit.reloadAll("C")
        assertEquals(listOf(true), handler.reloadAllClears)
    }

    @Test
    fun reload_writeOnBootTrue_unknownCacheName_noHandlerInvoked() {
        // handlersFor("OTHER") returns empty -> nothing happens, index still built (covers index lookup miss).
        provider.register("OTHER", true, CacheStrategy.REMOTE, writeOnBoot = true)
        KeyValueCacheKit.reload("OTHER", "k")
        assertTrue(handler.reloadKeys.isEmpty())
    }

    @Test
    fun evictByPattern_active_delegatesToManager() {
        provider.register("C", true, CacheStrategy.REMOTE)
        KeyValueCacheKit.evictByPattern("C", "p*")
        Mockito.verify(mockMgr).evictByPattern("C", "p*")
    }

    @Test
    fun existsKey_active_delegatesToManager() {
        provider.register("C", true, CacheStrategy.REMOTE)
        Mockito.`when`(mockMgr.existsKey("C", "k")).thenReturn(true)
        assertTrue(KeyValueCacheKit.existsKey("C", "k"))
    }

    @Test
    fun existsKey_active_managerReturnsFalse() {
        provider.register("C", true, CacheStrategy.REMOTE)
        Mockito.`when`(mockMgr.existsKey("C", "absent")).thenReturn(false)
        assertFalse(KeyValueCacheKit.existsKey("C", "absent"))
    }

    // ---- test doubles -------------------------------------------------------

    private class RecordingHandler(private val name: String) : AbstractKeyValueCacheHandler<String>() {
        val reloadKeys = mutableListOf<String>()
        val reloadAllClears = mutableListOf<Boolean>()
        override fun cacheName(): String = name
        override fun reload(key: String) { reloadKeys += key }
        override fun reloadAll(clear: Boolean) { reloadAllClears += clear }
        override fun doReload(key: String): String? = null
    }

    private class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean, strategy: CacheStrategy, writeOnBoot: Boolean = false) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name; this.writeOnBoot = writeOnBoot
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
