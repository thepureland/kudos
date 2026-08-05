package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.aop.FakeProceedingJoinPoint
import io.kudos.ability.cache.common.aop.methodOf
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [HashCacheableBySecondaryAspect] 单元测试。
 *
 * 通过手工构造的假 ProceedingJoinPoint 直接调用 around，逐分支覆盖：
 *  - 合成方法 / 无注解 / 无 cacheName / condition=false / condition 求值为 null / filterExpressions 为空 /
 *    类级 @CacheConfig 无 cacheNames → 直接 proceed
 *  - SINGLE_ENTITY 形态在缓存未激活、writeInTime=false 时的回写守卫
 *  - 非法 filterExpressions（复合表达式）→ IllegalArgumentException
 *  - SpEL 求值为 null → 直接 proceed
 *  - 四种返回形态（SINGLE_ID / LIST_IDS / LIST_ENTITIES / SINGLE_ENTITY）的命中路径，
 *    含 returnProperty 投影、Set 返回类型、多条件交集与空交集
 *  - 未命中路径：null 结果、unless 跳过回写、saveBatch 回写（含属性集合）、
 *    返回值不是 List / 列表元素不是实体 / 单对象不是实体时不回写
 *  - 缓存未激活、writeInTime=false 的开关分支
 *  - resolveReturnMode 对非参数化 List/Set 子类与 Set<非String> 的推断
 *
 * 依赖通过 [HashCacheKit.overrideForTesting] / [KeyValueCacheKit.overrideForTesting] 注入。
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HashCacheableBySecondaryAspectTest {

    private val aspect = HashCacheableBySecondaryAspect()
    private val hashCache = FakeHashCache()
    private val configProvider = ProgrammableCacheConfigProvider()

    @BeforeAll
    fun classSetup() {
        HashCacheKit.overrideForTesting(
            manager = buildHashManager(
                CACHE to hashCache,
                INACTIVE_CACHE to hashCache,
                NO_WRITE_CACHE to hashCache
            )
        )
        KeyValueCacheKit.overrideForTesting(configProvider = configProvider)
        configProvider.register(CACHE, active = true, writeInTime = true)
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
    fun classLevelCacheConfig_isUsedAsFallback() {
        val cached = listOf(HashEntity("cc1", type = "T"))
        hashCache.setIndex["type" to "T"] = cached
        val method = methodOf(ClassLevelSecondaryOps::class.java, "listByType")
        val pjp = FakeProceedingJoinPoint(method, arrayOf("T"), ClassLevelSecondaryOps()) { error("should not proceed") }

        assertEquals(cached, aspect.around(pjp), "应从类级 @CacheConfig 解析 cacheName 并命中缓存")
    }

    @Test
    fun syntheticMethod_proceeds() {
        val synthetic = SecondaryOps::class.java.declaredMethods.first { it.name == "withDefault\$default" }
        val pjp = FakeProceedingJoinPoint(synthetic, arrayOf(), SecondaryOps()) { "from-method" }

        assertEquals("from-method", aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount)
    }

    @Test
    fun methodWithoutAnnotation_proceeds() {
        val pjp = pjpOf("notAnnotated", "T") { listOf(HashEntity("1")) }

        assertEquals(listOf(HashEntity("1")), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount)
    }

    @Test
    fun noCacheName_proceeds() {
        val pjp = pjpOf("noCacheName", "T") { emptyList<HashEntity>() }

        assertEquals(emptyList<HashEntity>(), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount)
    }

    @Test
    fun conditionFalse_proceeds() {
        hashCache.setIndex["type" to "other"] = listOf(HashEntity("1", type = "other"))
        val pjp = pjpOf("conditional", "other") { emptyList<HashEntity>() }

        assertEquals(emptyList<HashEntity>(), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount, "condition 不满足时不应查索引")
    }

    @Test
    fun conditionResolvesNull_throwsSpelEvaluationException() {
        // condition 引用未定义变量 → SpEL 求值为 null；getValue(.., Boolean::class.java) 的目标是
        // 原生 boolean，null 无法转换 → 抛 SpelEvaluationException（记录当前行为：错误配置会显式失败，
        // 不会被静默当成 false。源码中 conditionResult != true 的 null 分支因此不可达）。
        hashCache.setIndex["type" to "T"] = listOf(HashEntity("1", type = "T"))
        val pjp = pjpOf("nullCondition", "T") { emptyList<HashEntity>() }

        assertFailsWith<org.springframework.expression.spel.SpelEvaluationException> { aspect.around(pjp) }
        assertEquals(0, pjp.proceedCount, "condition 求值失败时不应执行原方法")
        assertEquals(0, hashCache.listBySetIndexCount, "condition 求值失败时不应查索引")
    }

    @Test
    fun emptyClassLevelCacheConfig_fallsBackToProceed() {
        // 类级 @CacheConfig 存在但 cacheNames 为空 → 解析不到 cacheName，直接走原方法
        val method = methodOf(EmptyCacheConfigSecondaryOps::class.java, "listByType")
        val pjp = FakeProceedingJoinPoint(method, arrayOf("T"), EmptyCacheConfigSecondaryOps()) { "fallthrough" }

        assertEquals("fallthrough", aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount)
    }

    @Test
    fun emptyFilterExpressions_proceeds() {
        val pjp = pjpOf("emptyExpr", "T") { listOf(HashEntity("1")) }

        assertEquals(listOf(HashEntity("1")), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount)
    }

    @Test
    fun compoundFilterExpression_throwsIllegalArgument() {
        val pjp = FakeProceedingJoinPoint(
            methodOf(SecondaryOps::class.java, "badExpr"), arrayOf("a", "b"), SecondaryOps()
        ) { error("should not proceed") }

        val e = assertFailsWith<IllegalArgumentException> { aspect.around(pjp) }
        assertTrue(e.message!!.contains("single-parameter SpEL"), "异常消息应说明表达式约束，实际: ${e.message}")
    }

    @Test
    fun filterExpressionValueNull_proceeds() {
        val pjp = pjpOf("listByType", null) { listOf(HashEntity("1")) }

        assertEquals(listOf(HashEntity("1")), aspect.around(pjp))
        assertEquals(1, pjp.proceedCount)
        assertEquals(0, hashCache.listBySetIndexCount, "查询值为 null 时不应查索引")
    }

    // ---- 命中路径：四种返回形态 ---------------------------------------------

    @Test
    fun hit_singleId_returnsFirstEntityId() {
        hashCache.setIndex["type" to "T"] = listOf(HashEntity("id-1", type = "T"), HashEntity("id-2", type = "T"))
        val pjp = pjpOf("idByType", "T") { error("should not proceed") }

        assertEquals("id-1", aspect.around(pjp))
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun hit_listIds_returnsIdList() {
        hashCache.setIndex["type" to "T"] =
            listOf(HashEntity("a", type = "T"), HashEntity("b", type = "T"), HashEntity(null, type = "T"))
        val pjp = pjpOf("idsByType", "T") { error("should not proceed") }

        assertEquals(listOf("a", "b"), aspect.around(pjp), "null id 应被剔除")
    }

    @Test
    fun hit_listIds_withReturnProperty_projectsDistinctNonNull() {
        hashCache.setIndex["type" to "T"] = listOf(
            HashEntity("a", type = "T", code = "C1"),
            HashEntity("b", type = "T", code = "C1"),
            HashEntity("c", type = "T", code = "C2"),
            HashEntity("d", type = "T", code = null)
        )
        val pjp = pjpOf("codesByType", "T") { error("should not proceed") }

        assertEquals(listOf("C1", "C2"), aspect.around(pjp), "returnProperty 投影应去重并剔除 null")
    }

    @Test
    fun hit_idSetReturnType_returnsSet() {
        hashCache.setIndex["type" to "T"] = listOf(HashEntity("a", type = "T"), HashEntity("b", type = "T"))
        val pjp = pjpOf("idSetByType", "T") { error("should not proceed") }

        assertEquals(setOf("a", "b"), aspect.around(pjp))
    }

    @Test
    fun hit_listEntities_returnsCachedList() {
        val cached = listOf(HashEntity("a", type = "T"), HashEntity("b", type = "T"))
        hashCache.setIndex["type" to "T"] = cached
        val pjp = pjpOf("listByType", "T") { error("should not proceed") }

        assertEquals(cached, aspect.around(pjp))
        assertEquals(0, pjp.proceedCount)
    }

    @Test
    fun hit_singleEntity_returnsFirst() {
        val first = HashEntity("a", type = "T")
        hashCache.setIndex["type" to "T"] = listOf(first, HashEntity("b", type = "T"))
        val pjp = pjpOf("oneByType", "T") { error("should not proceed") }

        assertSame(first, aspect.around(pjp))
    }

    // ---- 多条件交集 ----------------------------------------------------------

    @Test
    fun multiKeyHit_intersectsByIdThenGetById() {
        val e2 = HashEntity("2", type = "T", code = "C")
        hashCache.setIndex["type" to "T"] = listOf(HashEntity("1", type = "T"), e2, HashEntity(null, type = "T"))
        hashCache.setIndex["code" to "C"] = listOf(e2, HashEntity("3", code = "C"))
        hashCache.entities["2"] = e2
        val pjp = FakeProceedingJoinPoint(
            methodOf(SecondaryOps::class.java, "listByTypeAndCode"), arrayOf("T", "C"), SecondaryOps()
        ) { error("should not proceed") }

        assertEquals(listOf(e2), aspect.around(pjp))
        assertEquals(2, hashCache.listBySetIndexCount, "两个条件应各查一次索引")
        assertEquals(1, hashCache.getByIdCount, "交集后应按 id 回查实体")
    }

    @Test
    fun multiKeyEmptyIntersection_fallsThroughToProceed() {
        hashCache.setIndex["type" to "T"] = listOf(HashEntity("1", type = "T"))
        hashCache.setIndex["code" to "C"] = listOf(HashEntity("2", code = "C"))
        val fresh = listOf(HashEntity("9", type = "T", code = "C"))
        val pjp = FakeProceedingJoinPoint(
            methodOf(SecondaryOps::class.java, "listByTypeAndCode"), arrayOf("T", "C"), SecondaryOps()
        ) { fresh }

        assertEquals(fresh, aspect.around(pjp), "空交集应视为未命中并走原方法")
        assertEquals(1, pjp.proceedCount)
    }

    // ---- 未命中路径与回写 ----------------------------------------------------

    @Test
    fun miss_nullResult_returnsNull() {
        val pjp = pjpOf("oneByType", "T") { null }

        assertNull(aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty())
    }

    @Test
    fun miss_unlessTrue_skipsSaveBatch() {
        val one = listOf(HashEntity("only", type = "T"))
        val pjp = pjpOf("withUnless", "T") { one }

        assertEquals(one, aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "unless 为 true 时不应回写")
    }

    @Test
    fun miss_unlessFalse_savesBatch() {
        val two = listOf(HashEntity("u1", type = "T"), HashEntity("u2", type = "T"))
        val pjp = pjpOf("withUnless", "T") { two }

        assertEquals(two, aspect.around(pjp))
        assertEquals(1, hashCache.saveBatchCalls.size, "unless 为 false 时应回写")
    }

    @Test
    fun miss_listEntities_savesBatchWithProps() {
        val fresh = listOf(HashEntity("1", type = "T", score = 1), HashEntity("2", type = "T", score = 2))
        val pjp = pjpOf("listByType", "T") { fresh }

        assertEquals(fresh, aspect.around(pjp))
        val (saved, filterable, sortable) = hashCache.saveBatchCalls.single()
        assertEquals(fresh, saved)
        assertEquals(setOf("type"), filterable)
        assertEquals(setOf("score"), sortable)
    }

    @Test
    fun miss_resultNotAList_returnsAsIs_withoutSave() {
        // 假 pjp 让 proceed 返回非 List 对象，覆盖 (result as? List<*>) ?: return result 分支
        val pjp = pjpOf("listByType", "T") { "not-a-list" }

        assertEquals("not-a-list", aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty())
    }

    @Test
    fun miss_listOfNonEntities_skipsSaveBatch() {
        val strings = listOf("x", "y")
        val pjp = pjpOf("listByType", "T") { strings }

        assertEquals(strings, aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "列表元素不是 IIdEntity 时不应回写")
    }

    @Test
    fun miss_singleId_returnsResultWithoutSave() {
        val pjp = pjpOf("idByType", "T") { "fresh-id" }

        assertEquals("fresh-id", aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "SINGLE_ID 形态回写由方法体负责")
    }

    @Test
    fun miss_listIds_returnsResultWithoutSave() {
        val pjp = pjpOf("idsByType", "T") { listOf("a", "b") }

        assertEquals(listOf("a", "b"), aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "LIST_IDS 形态回写由方法体负责")
    }

    @Test
    fun miss_singleEntity_savesViaSaveBatch() {
        val one = HashEntity("s1", type = "T")
        val pjp = pjpOf("oneByType", "T") { one }

        assertSame(one, aspect.around(pjp))
        val (saved, _, _) = hashCache.saveBatchCalls.single()
        assertEquals(listOf(one), saved)
    }

    @Test
    fun miss_singleNonEntity_skipsSave() {
        val pjp = pjpOf("intSetReturn", "T") { setOf(1, 2) }

        assertEquals(setOf(1, 2), aspect.around(pjp), "Set<非String> 应按 SINGLE_ENTITY 处理且不回写")
        assertTrue(hashCache.saveBatchCalls.isEmpty())
    }

    // ---- 开关分支 ------------------------------------------------------------

    @Test
    fun inactiveCache_skipsQueryAndWrite() {
        hashCache.setIndex["type" to "T"] = listOf(HashEntity("1", type = "T"))
        val fresh = listOf(HashEntity("9", type = "T"))
        val pjp = pjpOf("inactiveCache", "T") { fresh }

        assertEquals(fresh, aspect.around(pjp))
        assertEquals(0, hashCache.listBySetIndexCount, "未激活时不应查索引")
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "未激活时不应回写")
    }

    @Test
    fun writeInTimeFalse_skipsSaveBatch() {
        val fresh = listOf(HashEntity("9", type = "T"))
        val pjp = pjpOf("noWriteCache", "T") { fresh }

        assertEquals(fresh, aspect.around(pjp))
        assertEquals(1, hashCache.listBySetIndexCount, "active 时仍应先查索引")
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "writeInTime=false 时不应回写")
    }

    @Test
    fun inactiveCache_singleEntityReturn_skipsWrite() {
        // SINGLE_ENTITY 形态走独立的回写守卫（与 LIST_ENTITIES 不同行），单独覆盖未激活分支
        val one = HashEntity("si1", type = "T")
        val pjp = pjpOf("oneInactive", "T") { one }

        assertSame(one, aspect.around(pjp))
        assertEquals(0, hashCache.listBySetIndexCount, "未激活时不应查索引")
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "未激活时单实体也不应回写")
    }

    @Test
    fun writeInTimeFalse_singleEntityReturn_skipsWrite() {
        val one = HashEntity("sw1", type = "T")
        val pjp = pjpOf("oneNoWrite", "T") { one }

        assertSame(one, aspect.around(pjp))
        assertEquals(1, hashCache.listBySetIndexCount, "active 时仍应先查索引")
        assertTrue(hashCache.saveBatchCalls.isEmpty(), "writeInTime=false 时单实体不应回写")
    }

    // ---- resolveReturnMode 边界 ----------------------------------------------

    @Test
    fun nonParameterizedListSubclass_treatedAsListEntities() {
        // StringList 的 genericReturnType 不是 ParameterizedType → LIST_ENTITIES；元素非实体则不回写
        val raw = StringList().apply { add("x") }
        val pjp = pjpOf("stringListReturn", "T") { raw }

        assertEquals(raw, aspect.around(pjp))
        assertTrue(hashCache.saveBatchCalls.isEmpty())
    }

    @Test
    fun nonParameterizedSetSubclass_treatedAsSingleEntity() {
        val raw = StringSet().apply { add("x") }
        val pjp = pjpOf("stringSetReturn", "T") { raw }

        assertEquals(raw, aspect.around(pjp), "非参数化 Set 子类应按 SINGLE_ENTITY 处理")
        assertTrue(hashCache.saveBatchCalls.isEmpty())
    }

    @Test
    fun hit_listIds_onListSubclassReturnType_fallsBackToEntities() {
        // 命中 + LIST_ENTITIES（StringList 推断为 LIST_ENTITIES）→ 返回缓存实体列表
        val cached = listOf(HashEntity("a", type = "T"))
        hashCache.setIndex["type" to "T"] = cached
        val pjp = pjpOf("stringListReturn", "T") { error("should not proceed") }

        assertEquals(cached, aspect.around(pjp))
    }

    // ---- 辅助 ---------------------------------------------------------------

    private fun pjpOf(methodName: String, arg: String?, proceedFn: () -> Any?): FakeProceedingJoinPoint =
        FakeProceedingJoinPoint(methodOf(SecondaryOps::class.java, methodName), arrayOf(arg), SecondaryOps(), proceedFn)

    internal class StringList : ArrayList<String>()
    internal class StringSet : HashSet<String>()

    /** 类级 @CacheConfig 提供 cacheName 回退。 */
    @org.springframework.cache.annotation.CacheConfig(cacheNames = [CACHE])
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class ClassLevelSecondaryOps {
        @HashCacheableBySecondary(filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun listByType(type: String): List<HashEntity> = emptyList()
    }

    /** 类级 @CacheConfig 存在但 cacheNames 为空（覆盖回退失败分支）。 */
    @org.springframework.cache.annotation.CacheConfig
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class EmptyCacheConfigSecondaryOps {
        @HashCacheableBySecondary(filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun listByType(type: String): List<HashEntity> = emptyList()
    }

    /** 注解持有类：方法体不会被执行，仅提供反射元数据。 */
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class SecondaryOps {

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class,
            filterableProperties = ["type"], sortableProperties = ["score"]
        )
        fun listByType(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun idByType(type: String): String? = null

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun idsByType(type: String): List<String> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class,
            returnProperty = "code"
        )
        fun codesByType(type: String): List<String> = emptyList()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun idSetByType(type: String): Set<String> = emptySet()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun oneByType(type: String): HashEntity? = null

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type", "#code"], entityClass = HashEntity::class
        )
        fun listByTypeAndCode(type: String, code: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = [], entityClass = HashEntity::class)
        fun emptyExpr(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type + #code"], entityClass = HashEntity::class
        )
        fun badExpr(type: String, code: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type"], condition = "#type == 'use'",
            entityClass = HashEntity::class
        )
        fun conditional(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type"], condition = "#missing",
            entityClass = HashEntity::class
        )
        fun nullCondition(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [INACTIVE_CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class
        )
        fun oneInactive(type: String): HashEntity? = null

        @HashCacheableBySecondary(
            cacheNames = [NO_WRITE_CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class
        )
        fun oneNoWrite(type: String): HashEntity? = null

        @HashCacheableBySecondary(
            cacheNames = [CACHE], filterExpressions = ["#type"], unless = "#result.size() == 1",
            entityClass = HashEntity::class
        )
        fun withUnless(type: String): List<HashEntity> = emptyList()

        fun notAnnotated(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun noCacheName(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [INACTIVE_CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class
        )
        fun inactiveCache(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(
            cacheNames = [NO_WRITE_CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class,
            filterableProperties = ["type"]
        )
        fun noWriteCache(type: String): List<HashEntity> = emptyList()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun stringListReturn(type: String): StringList = StringList()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun stringSetReturn(type: String): StringSet = StringSet()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun intSetReturn(type: String): Set<Int> = emptySet()

        @HashCacheableBySecondary(cacheNames = [CACHE], filterExpressions = ["#type"], entityClass = HashEntity::class)
        fun withDefault(type: String = "t"): List<HashEntity> = emptyList()
    }

    companion object {
        private const val CACHE = "HASH_SECONDARY_TEST"
        private const val INACTIVE_CACHE = "HASH_SECONDARY_INACTIVE_TEST"
        private const val NO_WRITE_CACHE = "HASH_SECONDARY_NO_WRITE_TEST"
    }
}
