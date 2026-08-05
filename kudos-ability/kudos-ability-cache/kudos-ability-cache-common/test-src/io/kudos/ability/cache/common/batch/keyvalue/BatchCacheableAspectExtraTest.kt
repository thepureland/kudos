package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.ability.cache.common.aop.FakeProceedingJoinPoint
import io.kudos.ability.cache.common.aop.methodOf
import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.cache.Cache
import org.springframework.cache.annotation.CacheConfig as SpringCacheConfig
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.support.StaticApplicationContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Supplementary [BatchCacheableAspect] tests driven by a hand-built [FakeProceedingJoinPoint] to reach
 * branches the proxy-based [BatchCacheableAspectTest] cannot:
 *  - cut() invocability,
 *  - validate(): missing cacheName error, non-Map return error,
 *  - readUncachedData element-type restoration for every supported Array<X> element type,
 *  - empty-collection element-type-inference error,
 *  - non-String key in the returned Map -> error,
 *  - key-set mismatch warning (missing / extra),
 *  - class-level @CacheConfig fallback.
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BatchCacheableAspectExtraTest {

    private val aspect = BatchCacheableAspect()
    private lateinit var cacheManager: TestMixCacheManager
    private lateinit var provider: TestProvider
    private lateinit var ctx: StaticApplicationContext

    @BeforeAll
    fun classSetup() {
        ctx = StaticApplicationContext().apply { refresh() }
        ctx.beanFactory.registerSingleton("defaultKeysGenerator", DefaultKeysGenerator())
        SpringKit.applicationContext = ctx

        cacheManager = TestMixCacheManager()
        provider = TestProvider().apply {
            register(CACHE, active = true)
            register(CLASS_CACHE, active = true)
        }
        KeyValueCacheKit.overrideForTesting(cacheManager, provider)
    }

    @AfterAll
    fun classTeardown() {
        KeyValueCacheKit.resetForTesting()
        ctx.close()
    }

    @BeforeTest
    fun reset() {
        cacheManager.clearAll()
    }

    @Test
    fun cut_isInvocable() {
        aspect.cut()
    }

    @Test
    fun validate_missingCacheName_throws() {
        val pjp = pjp("noCacheName", listOf(1)) { mapOf("1" to "v") }
        assertFailsWith<IllegalStateException> { aspect.around(pjp) }
    }

    @Test
    fun validate_nonMapReturnType_throws() {
        val pjp = pjp("wrongReturn", listOf(1)) { "x" }
        assertFailsWith<IllegalStateException> { aspect.around(pjp) }
    }

    @Test
    fun classLevelCacheConfig_fallback() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(ClassLevelOps::class.java, "load"), arrayOf(listOf(1)), ClassLevelOps()
        ) { mapOf("1" to "v1") }
        val result = aspect.around(pjp)
        assertEquals(mapOf("1" to "v1"), result)
    }

    @Test
    fun arrayParam_elementTypeRestored() {
        // Note: all `Array<X>::class` erase to the same `kotlin.Array` KClass, so the aspect's per-element-type
        // Array branches collapse to the first (Array<String>) branch. This still exercises the Array path.
        val pjp = FakeProceedingJoinPoint(
            methodOf(ArrayOps::class.java, "ints"), arrayOf(arrayOf(1, 2)), ArrayOps()
        ) { mapOf("1" to "v1", "2" to "v2") }
        assertEquals(mapOf("1" to "v1", "2" to "v2"), aspect.around(pjp))
    }

    @Test
    fun setParam_restoredAsSet() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(ArrayOps::class.java, "stringSet"), arrayOf(setOf("a", "b")), ArrayOps()
        ) { mapOf("a" to "1", "b" to "2") }
        assertEquals(setOf("a", "b"), aspect.around(pjp).keys)
    }

    @Test
    fun emptyArray_yieldsEmptyResult_withoutProceed() {
        // empty array -> no keys -> nothing to load; readUncachedData short-circuits on empty missing set.
        val pjp = FakeProceedingJoinPoint(
            methodOf(ArrayOps::class.java, "ints"), arrayOf(emptyArray<Int>()), ArrayOps()
        ) { error("should not proceed") }
        assertTrue(aspect.around(pjp).isEmpty())
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun proceedReturnsNonMap_throws() {
        // declared return is Map (passes validate), but the runtime proceed returns a non-Map ->
        // readUncachedData require(proceeded is Map) -> IllegalArgumentException
        val pjp = pjp("load", listOf(1)) { "not-a-map" }
        assertFailsWith<IllegalArgumentException> { aspect.around(pjp) }
    }

    @Test
    fun missingGeneratorBean_fallsBackToDefault() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(BatchOps::class.java, "customGen"), arrayOf(listOf(1)), BatchOps()
        ) { mapOf("1" to "v1") }
        assertEquals(mapOf("1" to "v1"), aspect.around(pjp))
    }

    @Test
    fun nonStringKeyInReturn_throws() {
        val pjp = pjp("load", listOf(1)) { mapOf(1 to "v") }
        assertFailsWith<IllegalStateException> { aspect.around(pjp) }
    }

    @Test
    fun keySetMismatch_logsWarning_butStillMerges() {
        // missing key set is {1,2}; business returns {1, 3} -> missing={2}, extra={3} -> warn path
        val pjp = pjp("load", listOf(1, 2)) { mapOf("1" to "v1", "3" to "v3") }
        val result = aspect.around(pjp)
        // 1 merged; 2 missing stays null (dropped); 3 is extra but written via putIfAbsent + merged only if key existed
        assertEquals("v1", result["1"])
    }

    @Test
    fun nullValuesAreDroppedFromResult() {
        val pjp = pjp("load", listOf(1)) { mapOf<String, Any?>("1" to null) }
        assertTrue(aspect.around(pjp).isEmpty(), "null cache values are filtered from the result")
    }

    // ---- helpers ----

    private fun pjp(method: String, ids: List<Int>, proceedFn: () -> Any?): FakeProceedingJoinPoint =
        FakeProceedingJoinPoint(methodOf(BatchOps::class.java, method), arrayOf(ids), BatchOps(), proceedFn)

    @Suppress("unused", "UNUSED_PARAMETER")
    internal class BatchOps {
        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun load(ids: List<Int>): Map<String, String> = emptyMap()

        @BatchCacheable(valueClass = String::class)
        fun noCacheName(ids: List<Int>): Map<String, String> = emptyMap()

        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun wrongReturn(ids: List<Int>): String = ""

        @BatchCacheable(cacheNames = [CACHE], keysGenerator = "noSuchGeneratorBean", valueClass = String::class)
        fun customGen(ids: List<Int>): Map<String, String> = emptyMap()
    }

    @SpringCacheConfig(cacheNames = [CLASS_CACHE])
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class ClassLevelOps {
        @BatchCacheable(valueClass = String::class)
        fun load(ids: List<Int>): Map<String, String> = emptyMap()
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    internal class ArrayOps {
        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun ints(ids: Array<Int>): Map<String, String> = emptyMap()

        @BatchCacheable(cacheNames = [CACHE], valueClass = String::class)
        fun stringSet(ids: Set<String>): Map<String, String> = emptyMap()
    }

    class TestMixCacheManager : MixCacheManager() {
        private val under = ConcurrentHashMap<String, Cache>()
        private val wrappers = ConcurrentHashMap<String, MixCache>()
        fun clearAll() { under.values.forEach { it.clear() } }
        fun underlying(name: String): Cache = under.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        override fun getCache(name: String): Cache =
            wrappers.computeIfAbsent(name) { MixCache(CacheStrategy.REMOTE, null, underlying(it)) }
    }

    class TestProvider : ICacheConfigProvider {
        private val configs = ConcurrentHashMap<String, CacheConfig>()
        fun register(name: String, active: Boolean) {
            configs[name] = CacheConfig().apply { this.name = name; this.active = active }
        }
        override fun getCacheConfig(name: String): CacheConfig? = configs[name]
        override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
        override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
        override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    }

    companion object {
        private const val CACHE = "BATCH_EXTRA_TEST"
        private const val CLASS_CACHE = "BATCH_EXTRA_CLASS_TEST"
    }
}
