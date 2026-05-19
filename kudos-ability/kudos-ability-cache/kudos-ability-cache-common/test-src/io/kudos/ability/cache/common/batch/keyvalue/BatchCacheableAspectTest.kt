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
 * [BatchCacheableAspect] "半命中合并" 行为单测。
 *
 * 核心契约（README）：
 *  - 部分 key 已在缓存 → 仅传**未命中** key 给业务方法
 *  - 业务方法返回的未命中数据写入缓存（`putIfAbsent`，不覆盖已存在的）
 *  - 返回值是"缓存中已有 + 业务新查"的合并 Map
 *
 * 用 [TestableMixCacheManager] + [InMemoryCacheConfigProvider] 通过 [KeyValueCacheKit.overrideForTesting]
 * 绕开真实 cache-common 自动配置——单测不需要 redis/caffeine。
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
        // 所有 3 个 key 都未命中，业务方法应一次性收到 3 个
        assertEquals(listOf(setOf(1, 2, 3)), log.snapshot())
        // 缓存现在应包含全部
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

        // 业务方法只收到未命中 key 集合（"3"）——单元素集合 → 入参 [3]
        assertEquals(listOf(setOf(3)), log.snapshot(),
            "业务方法只应被传入未命中的 key 集合")
        // 返回值合并：缓存中的 1/2 不被业务返回覆盖，3 来自业务
        assertEquals(
            mapOf("1" to "preloaded-v1", "2" to "preloaded-v2", "3" to "v3"),
            result,
            "缓存命中值 + 业务新查值应当合并返回"
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

        assertEquals(emptyList(), log.snapshot(), "全部命中时业务方法不应被调用")
        assertEquals(mapOf("1" to "c1", "2" to "c2"), result)
    }

    @Test
    fun putIfAbsent_doesNotOverwriteExistingCacheValue() {
        val cache = cacheManager.getOrCreate(CACHE_NAME)
        cache.put("1", "cached-1")
        // 业务方法将返回 "v1" — putIfAbsent 应当不覆盖

        val service = ctx.getBean(BatchedService::class.java)
        // service.loadAll([1, 2]) 触发缓存 1 命中、2 未命中
        service.loadAll(listOf(1, 2))

        // cached-1 应保持原值（putIfAbsent 不覆盖）
        assertEquals("cached-1", cache.get("1")?.get(),
            "putIfAbsent 不应覆盖已有的缓存值")
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

    /** 外置的调用记录器——业务 service 是 CGLIB 代理目标，类内 field initializer 在代理路径下可能返回 null。 */
    open class CallLog {
        private val entries = mutableListOf<Set<Int>>()
        fun record(keys: Set<Int>) { entries.add(keys) }
        fun snapshot(): List<Set<Int>> = entries.toList()
        fun clear() { entries.clear() }
    }

    /** 业务侧的 batch service：返回 Map<String, String>。 */
    @Service
    open class BatchedService(val log: CallLog) {
        @BatchCacheable(cacheNames = [CACHE_NAME], valueClass = String::class)
        open fun loadAll(ids: List<Int>): Map<String, String> {
            log.record(ids.toSet())
            return ids.associate { it.toString() to "v$it" }
        }
    }

    /** 最小可用的 [MixCacheManager]：用 [ConcurrentMapCache] 当 backing store，避免拉起完整 kudos 装配。 */
    class TestableMixCacheManager : MixCacheManager() {
        private val caches = ConcurrentHashMap<String, Cache>()

        fun getOrCreate(name: String): Cache = caches.computeIfAbsent(name) { ConcurrentMapCache(it, true) }

        fun clearAll() {
            caches.values.forEach { it.clear() }
        }

        // 重写 getCache 绕过父类 versionConfig/initializeCaches 路径
        override fun getCache(name: String): Cache = getOrCreate(name)
    }

    /** 最小可用的 [ICacheConfigProvider]：返回 active CacheConfig 即可让 KeyValueCacheKit 不短路。 */
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
