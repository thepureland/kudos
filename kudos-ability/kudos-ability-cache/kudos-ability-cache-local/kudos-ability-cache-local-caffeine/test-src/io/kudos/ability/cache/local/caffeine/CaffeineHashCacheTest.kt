package io.kudos.ability.cache.local.caffeine

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import kotlin.test.*

/**
 * [CaffeineHashCache] 纯单元测试。
 *
 * 不依赖 Spring 容器，直接实例化，覆盖：
 * - 构造参数校验（maximumSize 非正数）
 * - save / saveBatch / getById / existsById / findByIds / listAll / deleteById / evictLocal / clearLocal / clear
 * - 主键规范化（trim、非字符串主键）
 * - save 时 entity.id 为 null 的异常分支
 * - 二级索引（Set / ZSet）的写入、覆盖、删除、淘汰联动
 * - getPropertyValue 的 field / getXxx / isXxx / 不存在 四条路径
 * - toDouble 的 Number / 数字字符串 / 非数字字符串 / 其他类型 分支
 * - list 的 criteria（EQ/IN/GT/GE/LT/LE/else 操作符、OR 组、嵌套 Criteria、null 值 acceptNull）、
 *   排序（升/降、属性值缺失回退）、分页（pageNo/pageSize 边界钳制）
 * - listPageByZSetIndex 的 desc/asc、offset/limit、索引不存在分支
 *
 * @author K
 * @since 1.0.0
 */
internal class CaffeineHashCacheTest {

    private val cacheName = "unit"

    private fun newCache(maximumSize: Long = CaffeineHashCache.DEFAULT_MAXIMUM_SIZE) = CaffeineHashCache(maximumSize)

    // ---------- constructor ----------

    @Test
    fun constructor_rejectsNonPositiveMaximumSize() {
        val exZero = assertFailsWith<IllegalArgumentException> { CaffeineHashCache(0) }
        assertTrue(exZero.message!!.contains("maximumSize must be positive"))
        assertFailsWith<IllegalArgumentException> { CaffeineHashCache(-1) }
        // 默认值可正常构造
        assertEquals(10_000L, CaffeineHashCache.DEFAULT_MAXIMUM_SIZE)
    }

    // ---------- save / getById / existsById ----------

    @Test
    fun save_throwsWhenEntityIdIsNull() {
        val cache = newCache()
        val ex = assertFailsWith<IllegalArgumentException> {
            cache.save(cacheName, NullableIdRow(id = null), emptySet(), emptySet())
        }
        assertEquals("entity.id must not be null", ex.message)
    }

    @Test
    fun save_normalizesPrimaryKeyByTrimming() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "  pk1  ", name = "n"), emptySet(), emptySet())
        // CHAR 型主键尾部空白被规范化，用 trim 后的主键可命中
        assertNotNull(cache.getById(cacheName, "pk1", Row::class))
        assertTrue(cache.existsById(cacheName, "pk1"))
        assertTrue(cache.existsById(cacheName, " pk1 "))
    }

    @Test
    fun existsById_falseWhenAbsent() {
        val cache = newCache()
        assertFalse(cache.existsById(cacheName, "absent"))
        assertNull(cache.getById(cacheName, "absent", Row::class))
    }

    @Test
    fun getById_unicodeAndLongKey() {
        val cache = newCache()
        val unicodeId = "键😀-" + "x".repeat(2048)
        cache.save(cacheName, Row(id = unicodeId, name = "u"), emptySet(), emptySet())
        assertEquals("u", cache.getById(cacheName, unicodeId, Row::class)?.name)
    }

    // ---------- saveBatch / findByIds / listAll ----------

    @Test
    fun saveBatch_emptyListIsNoop() {
        val cache = newCache()
        cache.saveBatch(cacheName, emptyList<Row>(), emptySet(), emptySet())
        assertTrue(cache.listAll(cacheName, Row::class).isEmpty())
    }

    @Test
    fun findByIds_emptyAndMissingIds() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "1"), emptySet(), emptySet())
        assertTrue(cache.findByIds(cacheName, emptyList<String>(), Row::class).isEmpty())
        val found = cache.findByIds(cacheName, listOf("1", "missing", null), Row::class)
        assertEquals(listOf("1"), found.map { it.id })
    }

    // ---------- deleteById / evictLocal / clearLocal / clear ----------

    @Test
    fun deleteById_returnsSilentlyWhenCacheNameAbsent() {
        val cache = newCache()
        // mainData 中无此 cacheName，走提前 return 分支，不抛异常
        cache.deleteById("noSuchCache", "1", Row::class, emptySet(), emptySet())
    }

    @Test
    fun evictLocal_removesEntryAndIndexes() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "1", type = 7), setOf("type"), setOf("type"))
        cache.evictLocal(cacheName, "1")
        assertNull(cache.getById(cacheName, "1", Row::class))
        assertTrue(cache.listBySetIndex(cacheName, Row::class, "type", 7).isEmpty())
    }

    @Test
    fun evictLocal_returnsSilentlyWhenCacheNameAbsent() {
        newCache().evictLocal("noSuchCache", "1")
    }

    @Test
    fun clearLocal_clearsDataAndIndexes_andToleratesAbsentName() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "1", type = 1), setOf("type"), setOf("type"))
        cache.clearLocal(cacheName)
        assertTrue(cache.listAll(cacheName, Row::class).isEmpty())
        assertTrue(cache.listBySetIndex(cacheName, Row::class, "type", 1).isEmpty())
        // 不存在的 cacheName：remove 返回 null，?.let 跳过，不抛异常
        cache.clearLocal("noSuchCache")
    }

    @Test
    fun clear_delegatesToClearLocal() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "1"), emptySet(), emptySet())
        cache.clear(cacheName)
        assertNull(cache.getById(cacheName, "1", Row::class))
    }

    // ---------- getPropertyValue 路径（通过二级索引间接验证） ----------

    @Test
    fun propertyValue_resolvedViaGetterAndIsGetter() {
        val cache = newCache()
        cache.save(cacheName, MethodRow("m1"), setOf("code", "active", "missing"), emptySet())
        // getXxx 路径
        assertEquals(listOf("m1"), cache.listBySetIndex(cacheName, MethodRow::class, "code", "code-m1").map { it.id })
        // isXxx 路径
        assertEquals(listOf("m1"), cache.listBySetIndex(cacheName, MethodRow::class, "active", true).map { it.id })
        // 不存在的属性：getPropertyValue 返回 null，不写索引
        assertTrue(cache.listBySetIndex(cacheName, MethodRow::class, "missing", "x").isEmpty())
    }

    @Test
    fun listBySetIndex_emptyWhenIndexAbsent() {
        val cache = newCache()
        assertTrue(cache.listBySetIndex(cacheName, Row::class, "type", 99).isEmpty())
    }

    // ---------- toDouble 分支 + listPageByZSetIndex ----------

    @Test
    fun zsetScore_supportsNumberNumericStringAndFallback() {
        val cache = newCache()
        // Number / 数字字符串 / 非数字字符串 / 其他类型（List）四种 score
        cache.save(cacheName, Row(id = "num", score = 5), emptySet(), setOf("score"))
        cache.save(cacheName, Row(id = "strNum", score = "3.5"), emptySet(), setOf("score"))
        cache.save(cacheName, Row(id = "strBad", score = "abc"), emptySet(), setOf("score"))
        cache.save(cacheName, Row(id = "other", score = listOf(1)), emptySet(), setOf("score"))
        cache.save(cacheName, Row(id = "neg", score = -1.5), emptySet(), setOf("score"))

        val asc = cache.listPageByZSetIndex(cacheName, Row::class, "score", 0, 10, desc = false).map { it.id }
        // 非数字与其他类型回退为 -Double.MAX_VALUE，排最前；负数排在回退值之后
        assertEquals(setOf("strBad", "other"), asc.take(2).toSet())
        assertEquals(listOf("neg", "strNum", "num"), asc.drop(2))

        val desc = cache.listPageByZSetIndex(cacheName, Row::class, "score", 0, 2, desc = true).map { it.id }
        assertEquals(listOf("num", "strNum"), desc)

        // offset / limit
        val page = cache.listPageByZSetIndex(cacheName, Row::class, "score", 1, 1, desc = true).map { it.id }
        assertEquals(listOf("strNum"), page)
    }

    @Test
    fun listPageByZSetIndex_emptyWhenIndexAbsent() {
        val cache = newCache()
        assertTrue(cache.listPageByZSetIndex(cacheName, Row::class, "nope", 0, 10).isEmpty())
    }

    @Test
    fun zsetIndex_scoreFromNullPropertyNotIndexed() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "nullScore", score = null), emptySet(), setOf("score"))
        assertTrue(cache.listPageByZSetIndex(cacheName, Row::class, "score", 0, 10).isEmpty())
    }

    // ---------- save 覆盖旧索引 / 容量淘汰联动（补充非字符串 score 场景） ----------

    @Test
    fun save_overwriteRemovesStaleIndexEntries() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "1", type = 1, score = 10), setOf("type"), setOf("score"))
        cache.save(cacheName, Row(id = "1", type = 2, score = 20), setOf("type"), setOf("score"))
        assertTrue(cache.listBySetIndex(cacheName, Row::class, "type", 1).isEmpty())
        assertEquals(listOf("1"), cache.listBySetIndex(cacheName, Row::class, "type", 2).map { it.id })
        assertEquals(listOf("1"), cache.listPageByZSetIndex(cacheName, Row::class, "score", 0, 10).map { it.id })
    }

    @Test
    fun maximumSizeEviction_keepsIndexesConsistent() {
        val cache = newCache(maximumSize = 1)
        cache.save(cacheName, Row(id = "1", type = 1, score = 1), setOf("type"), setOf("score"))
        cache.save(cacheName, Row(id = "2", type = 1, score = 2), setOf("type"), setOf("score"))
        val all = cache.listAll(cacheName, Row::class).map { it.id }.toSet()
        assertEquals(1, all.size)
        assertEquals(all, cache.listBySetIndex(cacheName, Row::class, "type", 1).map { it.id }.toSet())
        assertEquals(all, cache.listPageByZSetIndex(cacheName, Row::class, "score", 0, 10).map { it.id }.toSet())
    }

    // ---------- refreshAll ----------

    @Test
    fun refreshAll_replacesAllDataAndIndexes() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "old", type = 1), setOf("type"), emptySet())
        cache.refreshAll(cacheName, listOf(Row(id = "new", type = 2)), setOf("type"), emptySet())
        assertNull(cache.getById(cacheName, "old", Row::class))
        assertTrue(cache.listBySetIndex(cacheName, Row::class, "type", 1).isEmpty())
        assertEquals(listOf("new"), cache.listBySetIndex(cacheName, Row::class, "type", 2).map { it.id })
    }

    // ---------- list：criteria / 排序 / 分页 ----------

    private fun cacheWithRows(): CaffeineHashCache {
        val cache = newCache()
        cache.save(cacheName, Row(id = "1", name = "Alice", type = 1, score = 10), emptySet(), emptySet())
        cache.save(cacheName, Row(id = "2", name = "Bob", type = 2, score = 20), emptySet(), emptySet())
        cache.save(cacheName, Row(id = "3", name = "Carol", type = 3, score = 30), emptySet(), emptySet())
        return cache
    }

    @Test
    fun list_nullOrEmptyCriteriaReturnsAll() {
        val cache = cacheWithRows()
        assertEquals(3, cache.list(cacheName, Row::class, null, 1, 10).size)
        // 空 Criteria：isEmpty() 为 true，跳过过滤
        assertEquals(3, cache.list(cacheName, Row::class, Criteria(), 1, 10).size)
    }

    @Test
    fun list_eqCriterion() {
        val cache = cacheWithRows()
        val match = cache.list(cacheName, Row::class, Criteria.of("type", OperatorEnum.EQ, 2), 1, 10)
        assertEquals(listOf("2"), match.map { it.id })
        val noMatch = cache.list(cacheName, Row::class, Criteria.of("type", OperatorEnum.EQ, 99), 1, 10)
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun list_inCriterion() {
        val cache = cacheWithRows()
        val match = cache.list(cacheName, Row::class, Criteria.of("type", OperatorEnum.IN, listOf(1, 3)), 1, 10)
        assertEquals(setOf("1", "3"), match.map { it.id }.toSet())
        // IN 值不是集合：as? Collection 为 null -> 不匹配
        val nonCollection = cache.list(cacheName, Row::class, Criteria.of("type", OperatorEnum.IN, "1"), 1, 10)
        assertTrue(nonCollection.isEmpty())
    }

    @Test
    fun list_comparisonCriterions() {
        val cache = cacheWithRows()
        assertEquals(
            setOf("3"),
            cache.list(cacheName, Row::class, Criteria.of("score", OperatorEnum.GT, 20), 1, 10).map { it.id }.toSet()
        )
        assertEquals(
            setOf("2", "3"),
            cache.list(cacheName, Row::class, Criteria.of("score", OperatorEnum.GE, 20), 1, 10).map { it.id }.toSet()
        )
        assertEquals(
            setOf("1"),
            cache.list(cacheName, Row::class, Criteria.of("score", OperatorEnum.LT, 20), 1, 10).map { it.id }.toSet()
        )
        assertEquals(
            setOf("1", "2"),
            cache.list(cacheName, Row::class, Criteria.of("score", OperatorEnum.LE, 20), 1, 10).map { it.id }.toSet()
        )
    }

    @Test
    fun list_unlistedOperatorsDelegateToOperatorEnum() {
        val cache = cacheWithRows()
        // LIKE 不在显式分流之列，交由 OperatorEnum.compare 处理，得到真正的"包含"语义。
        // 旧实现的兜底分支是 toString 相等，会把 LIKE 静默降级成全等匹配。
        val match = cache.list(cacheName, Row::class, Criteria.of("name", OperatorEnum.LIKE, "Alice"), 1, 10)
        assertEquals(listOf("1"), match.map { it.id })
        val prefixMatch = cache.list(cacheName, Row::class, Criteria.of("name", OperatorEnum.LIKE, "Ali"), 1, 10)
        assertEquals(listOf("1"), prefixMatch.map { it.id }, "LIKE 应是包含匹配，而非全等")
    }

    @Test
    fun list_negationOperatorsAreNotInverted() {
        // 最危险的一类：NE / LG 落到旧的兜底分支后返回的是"相等"，
        // 即查"不等于 Alice"反而只返回 Alice —— 结果与语义完全相反，且无异常、无日志。
        val cache = cacheWithRows()
        val ne = cache.list(cacheName, Row::class, Criteria.of("name", OperatorEnum.NE, "Alice"), 1, 10)
        assertTrue(ne.none { it.id == "1" }, "NE 不应返回等于该值的记录，实际: ${ne.map { it.id }}")
        assertTrue(ne.isNotEmpty(), "NE 应返回其余记录")
    }

    @Test
    fun list_nullActualOrExpectedReturnsAcceptNull() {
        val cache = cacheWithRows()
        // actual 为 null（实体上无该属性）：EQ.acceptNull=false -> 全部不匹配
        val byMissingProp = cache.list(cacheName, Row::class, Criteria.of("noSuchProp", OperatorEnum.EQ, 1), 1, 10)
        assertTrue(byMissingProp.isEmpty())
        // expected 为 null：IS_NULL.acceptNull=true -> 全部匹配
        val isNull = cache.list(cacheName, Row::class, Criteria.of("type", OperatorEnum.IS_NULL, null), 1, 10)
        assertEquals(3, isNull.size)
        // actual 为 null 且 acceptNull=true
        val missingPropIsNull = cache.list(cacheName, Row::class, Criteria.of("noSuchProp", OperatorEnum.IS_NULL, null), 1, 10)
        assertEquals(3, missingPropIsNull.size)
    }

    @Test
    fun list_orGroupAndNestedCriteria() {
        val cache = cacheWithRows()
        // Array OR 组：type=1 OR type=3
        val orCriteria = Criteria().addOr(
            Criterion("type", OperatorEnum.EQ, 1),
            Criterion("type", OperatorEnum.EQ, 3)
        )
        assertEquals(
            setOf("1", "3"),
            cache.list(cacheName, Row::class, orCriteria, 1, 10).map { it.id }.toSet()
        )
        // OR 组内嵌套 Criteria：name=Bob OR (type>=3 AND score>=30)
        val orWithNested = Criteria.or(
            Criterion("name", OperatorEnum.EQ, "Bob"),
            Criteria.of("type", OperatorEnum.GE, 3).addAnd("score", OperatorEnum.GE, 30)
        )
        assertEquals(
            setOf("2", "3"),
            cache.list(cacheName, Row::class, orWithNested, 1, 10).map { it.id }.toSet()
        )
        // OR 组全不匹配 -> false 分支
        val orNoMatch = Criteria().addOr(
            Criterion("type", OperatorEnum.EQ, 98),
            Criterion("type", OperatorEnum.EQ, 99)
        )
        assertTrue(cache.list(cacheName, Row::class, orNoMatch, 1, 10).isEmpty())
        // 顶层嵌套 Criteria（AND）
        val nested = Criteria.and(Criteria.of("type", OperatorEnum.EQ, 1))
        assertEquals(listOf("1"), cache.list(cacheName, Row::class, nested, 1, 10).map { it.id })
        // 顶层嵌套 Criteria 不匹配 -> false 分支
        val nestedNoMatch = Criteria.and(Criteria.of("type", OperatorEnum.EQ, 99))
        assertTrue(cache.list(cacheName, Row::class, nestedNoMatch, 1, 10).isEmpty())
    }

    @Test
    fun list_orderAscDescAndNullFallback() {
        val cache = newCache()
        cache.save(cacheName, Row(id = "a", score = 2), emptySet(), emptySet())
        cache.save(cacheName, Row(id = "b", score = 1), emptySet(), emptySet())
        cache.save(cacheName, Row(id = "c", score = null), emptySet(), emptySet())
        val asc = cache.list(cacheName, Row::class, null, 1, 10, Order.asc("score")).map { it.id }
        // score 为 null 回退到 -Double.MAX_VALUE，升序排最前
        assertEquals(listOf("c", "b", "a"), asc)
        val desc = cache.list(cacheName, Row::class, null, 1, 10, Order.desc("score")).map { it.id }
        assertEquals(listOf("a", "b", "c"), desc)
    }

    @Test
    fun list_pageBoundsClampedToOne() {
        val cache = cacheWithRows()
        // pageNo=0、pageSize=0 分别被钳到 1
        val page = cache.list(cacheName, Row::class, null, 0, 0, Order.asc("score"))
        assertEquals(listOf("1"), page.map { it.id })
        val negative = cache.list(cacheName, Row::class, null, -3, -5, Order.asc("score"))
        assertEquals(listOf("1"), negative.map { it.id })
        // 正常翻页
        val page2 = cache.list(cacheName, Row::class, null, 2, 2, Order.asc("score"))
        assertEquals(listOf("3"), page2.map { it.id })
        // 超出范围的页返回空
        val page9 = cache.list(cacheName, Row::class, null, 9, 10, Order.asc("score"))
        assertTrue(page9.isEmpty())
    }

    // ---------- 并发冒烟：声明用 ConcurrentHashMap，做基本线程安全验证 ----------

    @Test
    fun concurrentSaveAndRead_doesNotThrow() {
        val cache = newCache()
        val threads = (1..4).map { t ->
            Thread {
                repeat(200) { i ->
                    val id = "t$t-$i"
                    cache.save(cacheName, Row(id = id, type = t, score = i), setOf("type"), setOf("score"))
                    cache.getById(cacheName, id, Row::class)
                    cache.listBySetIndex(cacheName, Row::class, "type", t)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(800, cache.listAll(cacheName, Row::class).size)
    }

    // ---------- 测试实体 ----------

    /** 普通字段实体：getPropertyValue 走 declaredField 路径 */
    data class Row(
        override var id: String = "",
        var name: String? = null,
        var type: Int? = null,
        var score: Any? = null
    ) : IIdEntity<String>

    /** 主键可空实体：用于 save 空主键异常分支 */
    class NullableIdRow(override val id: String?) : IIdEntity<String?>

    /** 只有方法没有字段的实体：getPropertyValue 走 getXxx / isXxx 路径 */
    class MethodRow(override val id: String) : IIdEntity<String> {
        @Suppress("unused")
        fun getCode(): String = "code-$id"

        @Suppress("unused")
        fun isActive(): Boolean = true
    }
}
