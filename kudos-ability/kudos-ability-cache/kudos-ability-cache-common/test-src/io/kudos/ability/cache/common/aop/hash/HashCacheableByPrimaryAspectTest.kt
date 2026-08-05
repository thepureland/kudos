package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.aop.FakeProceedingJoinPoint
import io.kudos.ability.cache.common.aop.methodOf
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.cache.annotation.CacheConfig
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [HashCacheableByPrimaryAspect] 单元测试。
 *
 * 通过手工构造的假 ProceedingJoinPoint 直接调用 around，逐分支覆盖：
 *  - 合成方法（kotlinFunction 为 null）/ 无注解 / 无 cacheName → 直接 proceed，不触缓存
 *  - cacheName 取自注解 cacheNames 或类级 @CacheConfig 的回退
 *  - condition 为 false / true 两路
 *  - key SpEL 求值为 null → 直接 proceed
 *  - 缓存命中 → 直接返回缓存实例，不 proceed
 *  - 未命中：结果为 null 不回写；unless 为 true 不回写；unless 为 false 回写
 *  - 回写时携带 filterable/sortable 属性集合
 *  - 缓存未激活 → 跳过查询与回写；writeInTime=false → 跳过回写
 *  - 返回值不是 IIdEntity → 不回写
 *
 * 依赖通过 [HashCacheKit.overrideForTesting] / [KeyValueCacheKit.overrideForTesting] 注入，
 * 不需要 Spring 容器与真实 redis/caffeine。
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HashCacheableByPrimaryAspectTest {

    private val aspect = HashCacheableByPrimaryAspect()
    private val hashCache = FakeHashCache()
    private val configProvider = ProgrammableCacheConfigProvider()

    @BeforeAll
    fun classSetup() {
        HashCacheKit.overrideForTesting(
            manager = buildHashManager(
                CACHE to hashCache,
                CLASS_CACHE to hashCache,
                INACTIVE_CACHE to hashCache,
                NO_WRITE_CACHE to hashCache
            )
        )
        KeyValueCacheKit.overrideForTesting(configProvider = configProvider)
        configProvider.register(CACHE, active = true, writeInTime = true)
        configProvider.register(CLASS_CACHE, active = true, writeInTime = true)
        configProvider.register(INACTIVE_CACHE, active = false)
        configProvider.register(NO_WRITE_CACHE, active = true, writeInTime = false)
    }

    @AfterAll
    fun classTeardown() {
        HashCacheKit.resetForTesting()
        KeyValueCacheKit.resetForTesting()
    }

    @BeforeTest
    fun reset() {
        hashCache.reset()
    }

    // ---- 前置短路分支 -------------------------------------------------------

    @Test
    fun pointcut_isInvocable() {
        // 切点方法本身是空体声明，Spring AOP 运行时不会执行它；直接调用以确认可达
        aspect.cut()
    }

    @Test
    fun classLevelCacheConfigWithEmptyNames_fallsBackToProceed() {
        val method = methodOf(EmptyCacheConfigOps::class.java, "byId")
        val pjp = FakeProceedingJoinPoint(method, arrayOf("e1"), EmptyCacheConfigOps()) { "fallthrough" }

        assertEquals("fallthrough", aspect.around(pjp), "类级 @CacheConfig 无 cacheNames 时应直接走原方法")
        assertEquals(1, pjp.proceedCount)
    }

    @Test
    fun syntheticMethod_proceedsWithoutCache() {
        // 带默认参数的函数会生成 withDefault$default 合成方法，其 kotlinFunction 为 null
        val synthetic = PrimaryOps::class.java.declaredMethods.first { it.name == "withDefault\$default" }
        val pjp = FakeProceedingJoinPoint(synthetic, arrayOf(), PrimaryOps()) { "from-method" }

        assertEquals("from-method", aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.getByIdCount)
    }

    @Test
    fun methodWithoutAnnotation_proceeds() {
        val pjp = pjpOf("notAnnotated", "1") { "plain" }

        assertEquals("plain", aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.getByIdCount)
    }

    @Test
    fun noCacheNameAnywhere_proceeds() {
        hashCache.entities["1"] = HashEntity("1")
        val pjp = pjpOf("noCacheName", "1") { HashEntity("1") }

        assertEquals(HashEntity("1"), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.getByIdCount, "未配置 cacheName 时不应触碰缓存")
    }

    @Test
    fun classLevelCacheConfig_isUsedAsFallback() {
        val cached = HashEntity("c1", type = "T")
        hashCache.entities["c1"] = cached
        val method = methodOf(ClassLevelOps::class.java, "byId")
        val pjp = FakeProceedingJoinPoint(method, arrayOf("c1"), ClassLevelOps()) { error("should not proceed") }

        assertSame(cached, aspect.around(pjp), "应从类级 @CacheConfig 解析 cacheName 并命中缓存")
        assertEquals(0, pjp.proceedCount)
    }

    // ---- condition / key 分支 ----------------------------------------------

    @Test
    fun conditionFalse_proceedsWithoutCache() {
        hashCache.entities["x"] = HashEntity("x")
        val pjp = pjpOf("conditional", "x") { HashEntity("fresh") }

        assertEquals(HashEntity("fresh"), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.getByIdCount, "condition 不满足时不应查缓存")
        assertTrue(hashCache.saveCalls.isEmpty(), "condition 不满足时不应回写")
    }

    @Test
    fun conditionTrue_hitsCache() {
        val cached = HashEntity("use", type = "T")
        hashCache.entities["use"] = cached
        val pjp = pjpOf("conditional", "use") { error("should not proceed") }

        assertSame(cached, aspect.around(pjp))
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun keyResolvesToNull_proceedsWithoutCache() {
        val pjp = pjpOf("nullKey", "1") { HashEntity("1") }

        assertEquals(HashEntity("1"), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.getByIdCount)
        assertTrue(hashCache.saveCalls.isEmpty(), "key 为 null 时不应回写")
    }

    // ---- 命中 / 未命中 ------------------------------------------------------

    @Test
    fun cacheHit_returnsCachedInstance_withoutProceed() {
        val cached = HashEntity("9", type = "T", score = 5)
        hashCache.entities["9"] = cached
        val pjp = pjpOf("byId", "9") { error("should not proceed") }

        assertSame(cached, aspect.around(pjp))
        assertEquals(0, pjp.proceedCount)
        assertEquals(1, hashCache.getByIdCount)
    }

    @Test
    fun cacheMiss_nullResult_returnsNull_withoutSave() {
        val pjp = pjpOf("byId", "404") { null }

        assertNull(aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertTrue(hashCache.saveCalls.isEmpty())
    }

    @Test
    fun cacheMiss_savesResult_withFilterableAndSortableProps() {
        val loaded = HashEntity("7", type = "T", score = 3)
        val pjp = pjpOf("byId", "7") { loaded }

        assertSame(loaded, aspect.around(pjp))
        assertEquals(1, hashCache.saveCalls.size)
        val (entity, filterable, sortable) = hashCache.saveCalls.single()
        assertSame(loaded, entity)
        assertEquals(setOf("type"), filterable)
        assertEquals(setOf("score"), sortable)
        assertSame(loaded, hashCache.entities["7"])
    }

    @Test
    fun unlessTrue_skipsSave() {
        val skipped = HashEntity("skip")
        val pjp = pjpOf("withUnless", "skip") { skipped }

        assertSame(skipped, aspect.around(pjp))
        assertTrue(hashCache.saveCalls.isEmpty(), "unless 为 true 时不应回写")
    }

    @Test
    fun unlessFalse_savesResult() {
        val kept = HashEntity("keep")
        val pjp = pjpOf("withUnless", "keep") { kept }

        assertSame(kept, aspect.around(pjp))
        assertEquals(1, hashCache.saveCalls.size)
    }

    // ---- 配置开关分支 -------------------------------------------------------

    @Test
    fun inactiveCache_skipsGetAndSave() {
        hashCache.entities["i1"] = HashEntity("i1")
        val fresh = HashEntity("i1", type = "fresh")
        val pjp = pjpOf("inactiveCache", "i1") { fresh }

        assertSame(fresh, aspect.around(pjp), "缓存未激活时应直接走原方法")
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.getByIdCount)
        assertTrue(hashCache.saveCalls.isEmpty())
    }

    @Test
    fun writeInTimeFalse_skipsSave() {
        val loaded = HashEntity("w1")
        val pjp = pjpOf("noWriteCache", "w1") { loaded }

        assertSame(loaded, aspect.around(pjp))
        assertEquals(1, hashCache.getByIdCount, "active 时仍应先查缓存")
        assertTrue(hashCache.saveCalls.isEmpty(), "writeInTime=false 时不应回写")
    }

    @Test
    fun resultNotIIdEntity_skipsSave() {
        // 假 pjp 可以让 proceed 返回与声明类型不同的对象，覆盖 result !is IIdEntity 分支
        val pjp = pjpOf("byId", "n1") { "not-an-entity" }

        assertEquals("not-an-entity", aspect.around(pjp))
        assertTrue(hashCache.saveCalls.isEmpty())
    }

    // ---- 辅助 ---------------------------------------------------------------

    private fun pjpOf(methodName: String, arg: String, proceedFn: () -> Any?): FakeProceedingJoinPoint =
        FakeProceedingJoinPoint(methodOf(PrimaryOps::class.java, methodName), arrayOf(arg), PrimaryOps(), proceedFn)

    /** 注解持有类：方法体不会被执行（proceed 由假 pjp 接管），仅提供反射元数据。 */
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class PrimaryOps {

        @HashCacheableByPrimary(
            cacheNames = [CACHE], key = "#id", entityClass = HashEntity::class,
            filterableProperties = ["type"], sortableProperties = ["score"]
        )
        fun byId(id: String): HashEntity? = null

        fun notAnnotated(id: String): HashEntity? = null

        @HashCacheableByPrimary(key = "#id", entityClass = HashEntity::class)
        fun noCacheName(id: String): HashEntity? = null

        @HashCacheableByPrimary(
            cacheNames = [CACHE], key = "#id", condition = "#id == 'use'", entityClass = HashEntity::class
        )
        fun conditional(id: String): HashEntity? = null

        @HashCacheableByPrimary(cacheNames = [CACHE], key = "#missing", entityClass = HashEntity::class)
        fun nullKey(id: String): HashEntity? = null

        @HashCacheableByPrimary(
            cacheNames = [CACHE], key = "#id", unless = "#result.id == 'skip'", entityClass = HashEntity::class
        )
        fun withUnless(id: String): HashEntity? = null

        @HashCacheableByPrimary(cacheNames = [INACTIVE_CACHE], key = "#id", entityClass = HashEntity::class)
        fun inactiveCache(id: String): HashEntity? = null

        @HashCacheableByPrimary(cacheNames = [NO_WRITE_CACHE], key = "#id", entityClass = HashEntity::class)
        fun noWriteCache(id: String): HashEntity? = null

        @HashCacheableByPrimary(cacheNames = [CACHE], key = "#id", entityClass = HashEntity::class)
        fun withDefault(id: String = "d"): HashEntity? = null
    }

    /** 类级 @CacheConfig 提供 cacheName 回退。 */
    @CacheConfig(cacheNames = [CLASS_CACHE])
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class ClassLevelOps {
        @HashCacheableByPrimary(key = "#id", entityClass = HashEntity::class)
        fun byId(id: String): HashEntity? = null
    }

    /** 类级 @CacheConfig 存在但 cacheNames 为空（覆盖回退失败分支）。 */
    @CacheConfig
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class EmptyCacheConfigOps {
        @HashCacheableByPrimary(key = "#id", entityClass = HashEntity::class)
        fun byId(id: String): HashEntity? = null
    }

    companion object {
        private const val CACHE = "HASH_PRIMARY_TEST"
        private const val CLASS_CACHE = "HASH_PRIMARY_CLASS_TEST"
        private const val INACTIVE_CACHE = "HASH_PRIMARY_INACTIVE_TEST"
        private const val NO_WRITE_CACHE = "HASH_PRIMARY_NO_WRITE_TEST"
    }
}
