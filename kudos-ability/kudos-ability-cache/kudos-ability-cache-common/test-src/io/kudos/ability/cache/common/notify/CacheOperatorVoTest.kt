package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CacheOperatorVo].
 *
 * Coverage:
 *  - Construction populates messageId / timestamp; nodeId starts null.
 *  - Constants TYPE_CLEAR / TYPE_EVICT.
 *  - [CacheOperatorVo.doNotify]:
 *      - notify succeeds (producer present) -> NO local fallback cleanup.
 *      - notify returns false (no producer, failOnMissingProducer=false) -> local fallback (clear / evict).
 *      - NotifyTool bean missing (NoSuchBeanDefinitionException) -> warn + local fallback.
 *      - nodeId is populated from the `cacheNodeId` bean when present, stays null otherwise.
 *  - Java serialization round-trips the payload.
 *
 * The fallback cleanup is verified through [KeyValueCacheKit.overrideForTesting] with an in-memory cache.
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheOperatorVoTest {

    private var ctx: AnnotationConfigApplicationContext? = null

    @AfterTest
    fun teardown() {
        KeyValueCacheKit.resetForTesting()
        ctx?.close()
        SpringKit.applicationContext = null
    }

    // ---- construction & constants -------------------------------------------

    @Test
    fun construction_populatesMessageIdAndTimestamp() {
        val before = System.currentTimeMillis()
        val vo = CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "C", "k")
        val after = System.currentTimeMillis()

        assertEquals(CacheOperatorVo.TYPE_EVICT, vo.type)
        assertEquals("C", vo.cacheName)
        assertEquals("k", vo.key)
        assertNotNull(vo.messageId, "messageId is generated at construction")
        assertTrue(vo.messageId!!.isNotBlank())
        assertNull(vo.nodeId, "nodeId is populated only at doNotify time")
        assertTrue(vo.timestamp in before..after, "timestamp should be the construction time")
    }

    @Test
    fun constants_haveExpectedValues() {
        assertEquals("clear", CacheOperatorVo.TYPE_CLEAR)
        assertEquals("evict", CacheOperatorVo.TYPE_EVICT)
    }

    @Test
    fun distinctInstances_haveDistinctMessageIds() {
        val a = CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null)
        val b = CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null)
        assertTrue(a.messageId != b.messageId, "each VO should get a unique messageId")
    }

    // ---- doNotify: success path (no fallback) -------------------------------

    @Test
    fun doNotify_whenNotifySucceeds_noLocalCleanup() {
        val mgr = TestMixCacheManager()
        val provider = TestProvider().apply { register("C", active = true, strategy = CacheStrategy.SINGLE_LOCAL) }
        KeyValueCacheKit.overrideForTesting(mgr, provider)
        mgr.underlying("C").put("k", "v")

        setSpringContext(SucceedingNotifyConfig::class.java)

        CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "C", "k").doNotify()

        // producer.notify returned true -> the local entry must remain untouched.
        assertEquals("v", mgr.underlying("C").get("k")?.get())
    }

    @Test
    fun doNotify_whenNotifySucceeds_populatesNodeIdFromBean() {
        val mgr = TestMixCacheManager()
        KeyValueCacheKit.overrideForTesting(mgr, TestProvider().apply { register("C", true, CacheStrategy.SINGLE_LOCAL) })
        setSpringContext(SucceedingNotifyConfig::class.java)

        val vo = CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null)
        vo.doNotify()

        assertEquals("node-X", vo.nodeId, "nodeId should be filled from the cacheNodeId bean")
    }

    // ---- doNotify: fallback path (notify returns false) ---------------------

    @Test
    fun doNotify_whenNotifyReturnsFalse_clearsLocally_forTypeClear() {
        val mgr = TestMixCacheManager()
        val provider = TestProvider().apply { register("C", true, CacheStrategy.REMOTE) }
        KeyValueCacheKit.overrideForTesting(mgr, provider)
        mgr.underlying("C").put("a", "1")
        mgr.underlying("C").put("b", "2")

        setSpringContext(FailingNotifyConfig::class.java)

        CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, "C", null).doNotify()

        assertNull(mgr.underlying("C").get("a"), "TYPE_CLEAR fallback should wipe the whole cache")
        assertNull(mgr.underlying("C").get("b"))
    }

    @Test
    fun doNotify_whenNotifyReturnsFalse_evictsLocally_forTypeEvict() {
        val mgr = TestMixCacheManager()
        val provider = TestProvider().apply { register("C", true, CacheStrategy.REMOTE) }
        KeyValueCacheKit.overrideForTesting(mgr, provider)
        mgr.underlying("C").put("k", "v")
        mgr.underlying("C").put("other", "keep")

        setSpringContext(FailingNotifyConfig::class.java)

        CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "C", "k").doNotify()

        assertNull(mgr.underlying("C").get("k"), "TYPE_EVICT fallback should remove only the given key")
        assertEquals("keep", mgr.underlying("C").get("other")?.get())
    }

    // ---- doNotify: NotifyTool bean missing ----------------------------------

    @Test
    fun doNotify_whenNotifyToolMissing_fallsBackToLocalCleanup() {
        val mgr = TestMixCacheManager()
        val provider = TestProvider().apply { register("C", true, CacheStrategy.REMOTE) }
        KeyValueCacheKit.overrideForTesting(mgr, provider)
        mgr.underlying("C").put("k", "v")

        // Context with NO NotifyTool bean -> SpringKit.getBean<NotifyTool>() throws NoSuchBeanDefinitionException.
        setSpringContext(EmptyConfig::class.java)

        val vo = CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "C", "k")
        vo.doNotify()

        assertNull(mgr.underlying("C").get("k"), "missing NotifyTool should degrade to local eviction")
        assertNull(vo.nodeId, "no cacheNodeId bean -> nodeId stays null")
    }

    // ---- serialization ------------------------------------------------------

    @Test
    fun javaSerialization_roundTripsPayload() {
        val original = CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, "ORDER", "k1").apply {
            nodeId = "n1"
        }
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as CacheOperatorVo }

        assertEquals(CacheOperatorVo.TYPE_EVICT, restored.type)
        assertEquals("ORDER", restored.cacheName)
        assertEquals("k1", restored.key)
        assertEquals("n1", restored.nodeId)
        assertEquals(original.messageId, restored.messageId)
        assertEquals(original.timestamp, restored.timestamp)
    }

    // ---- helpers ------------------------------------------------------------

    private fun setSpringContext(configClass: Class<*>) {
        ctx = AnnotationConfigApplicationContext(configClass)
        SpringKit.applicationContext = ctx
    }

    /** Context whose NotifyTool has a producer that returns true (notify succeeds). */
    @Configuration
    open class SucceedingNotifyConfig {
        @Bean
        open fun notifyProducer(): INotifyProducer = object : INotifyProducer {
            override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean = true
        }

        @Bean
        open fun notifyCommonProperties(): NotifyCommonProperties = NotifyCommonProperties()

        @Bean
        open fun notifyTool(producer: INotifyProducer, props: NotifyCommonProperties): NotifyTool =
            NotifyTool(producer, props)

        @Bean("cacheNodeId")
        open fun cacheNodeId(): String = "node-X"
    }

    /** Context whose NotifyTool has no producer and failOnMissingProducer=false -> notify returns false. */
    @Configuration
    open class FailingNotifyConfig {
        @Bean
        open fun notifyCommonProperties(): NotifyCommonProperties =
            NotifyCommonProperties().apply { failOnMissingProducer = false }

        @Bean
        open fun notifyTool(props: NotifyCommonProperties): NotifyTool = NotifyTool(null, props)
    }

    /** Context with no NotifyTool / cacheNodeId at all. */
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
        fun register(name: String, active: Boolean, strategy: CacheStrategy) {
            configs[name] = CacheConfig().apply {
                this.name = name; this.active = active; this.strategy = strategy.name
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }
}
