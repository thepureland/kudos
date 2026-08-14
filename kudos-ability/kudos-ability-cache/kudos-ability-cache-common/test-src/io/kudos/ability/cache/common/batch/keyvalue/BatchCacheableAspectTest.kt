package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [BatchCacheableAspect] "partial-hit merging" behavior.
 *
 * Core contract (README):
 *  - Some keys already cached → only the **missing** keys are passed to the business method.
 *  - The missing data returned by the business method is written to the cache (`putIfAbsent`, not overwriting existing entries).
 *  - The return value is the merged Map of "what's already cached + freshly queried by the business method".
 *
 * Uses [TestableMixCacheManager] + [InMemoryCacheConfigProvider] through [KeyValueCacheKit.overrideForTesting]
 * to bypass the real cache-common auto-configuration — unit tests don't need redis/caffeine.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BatchCacheableAspectTest {

    private lateinit var ctx: AnnotationConfigApplicationContext
    private lateinit var cacheManager: TestableMixCacheManager
    private lateinit var configProvider: InMemoryCacheConfigProvider

    @BeforeAll
    fun classSetup() {
        ctx = AnnotationConfigApplicationContext()
        ctx.register(TestAopConfig::class.java)
        SpringKit.applicationContext = ctx
        ctx.refresh()

        cacheManager = TestableMixCacheManager()
        configProvider = InMemoryCacheConfigProvider().apply {
            register(CACHE_NAME, active = true)
        }
        KeyValueCacheKit.overrideForTesting(cacheManager, configProvider)
    }

    @AfterAll
    fun classTeardown() {
        KeyValueCacheKit.resetForTesting()
        ctx.close()
    }

    @BeforeTest
    fun reset() {
        cacheManager.clearAll()
        ctx.getBean(CallLog::class.java).clear()
    }

    @Test
    fun allMissing_invokesMethodWithAllKeys_thenCachesAll() {
        val service = ctx.getBean(BatchedService::class.java)
        val log = ctx.getBean(CallLog::class.java)

        val result = service.loadAll(listOf(1, 2, 3))

        assertEquals(mapOf("1" to "v1", "2" to "v2", "3" to "v3"), result)
        // All 3 keys miss, so the business method should receive all 3 in a single call.
        assertEquals(listOf(setOf(1, 2, 3)), log.snapshot())
        // The cache should now contain all of them.
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        assertEquals("v1", cache.get("1")?.get())
        assertEquals("v3", cache.get("3")?.get())
    }

    @Test
    fun halfHit_invokesMethodOnlyWithMissing_thenMergesResult() {
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        cache.put("1", "preloaded-v1")
        cache.put("2", "preloaded-v2")

        val service = ctx.getBean(BatchedService::class.java)
        val log = ctx.getBean(CallLog::class.java)
        val result = service.loadAll(listOf(1, 2, 3))

        // The business method only receives the missing key set ("3") — a single-element set → arg [3].
        assertEquals(listOf(setOf(3)), log.snapshot(),
            "The business method should only receive the set of missing keys")
        // Return value merges: cached 1/2 are not overridden by business-returned values; 3 comes from the business call.
        assertEquals(
            mapOf("1" to "preloaded-v1", "2" to "preloaded-v2", "3" to "v3"),
            result,
            "Cached values + freshly queried values should be merged in the return value"
        )
    }

    @Test
    fun allHit_skipsBusinessCallEntirely() {
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        cache.put("1", "c1")
        cache.put("2", "c2")

        val service = ctx.getBean(BatchedService::class.java)
        val log = ctx.getBean(CallLog::class.java)
        val result = service.loadAll(listOf(1, 2))

        assertEquals(emptyList(), log.snapshot(), "Business method should not be called when all keys hit")
        assertEquals(mapOf("1" to "c1", "2" to "c2"), result)
    }

    @Test
    fun keysContainingTheDelimiter_stillSelectTheRightMissingElements() {
        // 参数值本身含有 key 分隔符 "::"。旧实现按分隔符切分 key 再反推参数，切出来的段数不对，
        // 会把错误的值传给业务方法；改为按位置从原始参数取元素后，分隔符不再参与其中。
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        cache.put("a::1", "cached-a")

        val service = ctx.getBean(BatchedService::class.java)
        val log = ctx.getBean(CallLog::class.java)
        val result = service.loadAllByCode(listOf("a::1", "b::2"))

        assertEquals(
            listOf<Set<Any>>(setOf("b::2")), log.snapshot(),
            "只应把未命中的原始元素回传给业务方法，且保持原值不被切碎"
        )
        assertEquals(mapOf("a::1" to "cached-a", "b::2" to "v-b::2"), result)
    }

    @Test
    fun cachedNullCountsAsHit_andIsNotReturned() {
        // 负缓存：命中的 null 说明"源端确实没有这条"，不应再回源。
        // 旧实现用 null 同时表示"未命中"和"命中且值为 null"，于是这类 key 每次调用都会重新回源。
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        cache.put("1", "c1")
        cache.put("2", null)

        val service = ctx.getBean(BatchedService::class.java)
        val log = ctx.getBean(CallLog::class.java)
        val result = service.loadAll(listOf(1, 2))

        assertEquals(emptyList(), log.snapshot(), "命中的 null 也是命中，不应再触发回源")
        assertEquals(
            mapOf("1" to "c1"), result,
            "返回值里要剔除 null —— 被注解方法声明的返回类型是值非空的 Map"
        )
    }

    @Test
    fun putIfAbsent_doesNotOverwriteExistingCacheValue() {
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        cache.put("1", "cached-1")
        // The business method will return "v1" — putIfAbsent must not overwrite.

        val service = ctx.getBean(BatchedService::class.java)
        // service.loadAll([1, 2]) triggers cache: 1 hits, 2 misses.
        service.loadAll(listOf(1, 2))

        // cached-1 should keep its original value (putIfAbsent does not overwrite).
        assertEquals("cached-1", cache.get("1")?.get(),
            "putIfAbsent must not overwrite an existing cache value")
        assertEquals("v2", cache.get("2")?.get())
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    open class TestAopConfig {
        @Bean
        open fun batchAspect(): BatchCacheableAspect = BatchCacheableAspect()

        @Bean("defaultKeysGenerator")
        open fun keysGenerator(): IKeysGenerator = DefaultKeysGenerator()

        @Bean
        open fun callLog(): CallLog = CallLog()

        @Bean
        open fun batchedService(log: CallLog): BatchedService = BatchedService(log)
    }

    /** External call recorder — the business service is a CGLIB proxy target, and in-class field initializers can return null on the proxy path. */
    open class CallLog {
        // Set<Any> rather than Set<Int>: some fixtures batch over String codes.
        private val entries = mutableListOf<Set<Any>>()
        fun record(keys: Set<Any>) { entries.add(keys) }
        fun snapshot(): List<Set<Any>> = entries.toList()
        fun clear() { entries.clear() }
    }

    /** Business-side batch service: returns Map<String, String>. */
    @Service
    open class BatchedService(val log: CallLog) {
        @BatchCacheable(cacheNames = [CACHE_NAME], valueClass = String::class)
        open fun loadAll(ids: List<Int>): Map<String, String> {
            log.record(ids.toSet())
            return ids.associate { it.toString() to "v$it" }
        }

        /** Ids whose own text contains the key delimiter `::`, used to pin the key-splitting regression. */
        @BatchCacheable(cacheNames = [CACHE_NAME], valueClass = String::class)
        open fun loadAllByCode(codes: List<String>): Map<String, String> {
            log.record(codes.toSet())
            return codes.associate { it to "v-$it" }
        }
    }

    /** Minimal usable [MixCacheManager]: backed by [ConcurrentMapCache] so we don't need to bring up full kudos wiring. */
    class TestableMixCacheManager : MixCacheManager() {
        private val caches = ConcurrentHashMap<String, Cache>()

        fun getOrCreate(name: String): Cache = caches.computeIfAbsent(name) { ConcurrentMapCache(it, true) }

        fun clearAll() {
            caches.values.forEach { it.clear() }
        }

        // Override getCache to bypass the parent class's versionConfig/initializeCaches path.
        override fun getCache(name: String): Cache = getOrCreate(name)
    }

    /** Minimal usable [ICacheConfigProvider]: returning an active CacheConfig is enough to keep KeyValueCacheKit from short-circuiting. */
    class InMemoryCacheConfigProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean) {
            configs[name] = CacheConfig().apply {
                this.name = name
                this.active = active
            }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }

    companion object {
        private const val CACHE_NAME = "BATCH_TEST"
    }
}
