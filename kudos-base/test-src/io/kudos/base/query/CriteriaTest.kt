package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Criteria测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class CriteriaTest {

    @Test
    fun testDefaultConstructor() {
        val criteria = Criteria()
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testConstructorWithPropertyOperatorValue() {
        val criteria = Criteria("name", OperatorEnum.EQ, "test")
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testConstructorWithCriterion() {
        val criterion = Criterion("age", OperatorEnum.GT, 18)
        val criteria = Criteria(criterion)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndWithPropertyOperatorValue() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "test")
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndWithCriterion() {
        val criteria = Criteria()
            .addAnd(Criterion("name", OperatorEnum.EQ, "test"))
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndMultipleCriterions() {
        val criteria = Criteria()
            .addAnd(
                Criterion("name", OperatorEnum.EQ, "test"),
                Criterion("age", OperatorEnum.GT, 18)
            )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndMultipleCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test")
        val criteria2 = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria()
            .addAnd(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val nestedCriteria = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria()
            .addAnd(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndCriteriaAndCriterion() {
        val nestedCriteria = Criteria("age", OperatorEnum.GT, 18)
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val criteria = Criteria()
            .addAnd(nestedCriteria, criterion)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrWithCriterions() {
        val criteria = Criteria()
            .addOr(
                Criterion("name", OperatorEnum.EQ, "test1"),
                Criterion("name", OperatorEnum.EQ, "test2")
            )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrWithCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test1")
        val criteria2 = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria()
            .addOr(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test1")
        val nestedCriteria = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria()
            .addOr(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrCriteriaAndCriterion() {
        val nestedCriteria = Criteria("name", OperatorEnum.EQ, "test1")
        val criterion = Criterion("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria()
            .addOr(nestedCriteria, criterion)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testComplexCriteria() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "test")
            .addAnd("age", OperatorEnum.GT, 18)
            .addOr(
                Criterion("status", OperatorEnum.EQ, "active"),
                Criterion("status", OperatorEnum.EQ, "pending")
            )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testNestedCriteria() {
        val innerCriteria = Criteria("age", OperatorEnum.GT, 18)
            .addAnd("age", OperatorEnum.LT, 65)
        val outerCriteria = Criteria("name", OperatorEnum.EQ, "test")
            .addAnd(innerCriteria)
        assertFalse(outerCriteria.isEmpty())
    }

    @Test
    fun testEmptyStringValueIsFiltered() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "")
        // 空字符串应该被过滤掉
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testNullValueWithAcceptNullOperator() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.IS_NULL, null)
        // IS_NULL操作符acceptNull为true，应该被添加
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testNullValueWithoutAcceptNullOperator() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, null)
        // EQ操作符acceptNull为false，null值应该被过滤
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testEmptyCollectionIsFiltered() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, emptyList<Int>())
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testNonEmptyCollectionIsAdded() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, listOf(1, 2, 3))
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testEmptyArrayIsFiltered() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, emptyArray<Int>())
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testNonEmptyArrayIsAdded() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, arrayOf(1, 2, 3))
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testEmptyNestedCriteriaIsFiltered() {
        val emptyCriteria = Criteria()
        val criteria = Criteria()
            .addAnd(emptyCriteria)
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testGetCriterionGroups() {
        val criteria = Criteria("name", OperatorEnum.EQ, "test")
        val groups = criteria.getCriterionGroups()
        assertFalse(groups.isEmpty())
    }

    @Test
    fun testToString() {
        val criteria = Criteria("name", OperatorEnum.EQ, "test")
            .addAnd("age", OperatorEnum.GT, 18)
        val result = criteria.toString()
        assertTrue(result.contains("name"))
        assertTrue(result.contains("age"))
        assertTrue(result.contains("test"))
    }

    @Test
    fun testToStringWithOr() {
        val criteria = Criteria()
            .addOr(
                Criterion("name", OperatorEnum.EQ, "test1"),
                Criterion("name", OperatorEnum.EQ, "test2")
            )
        val result = criteria.toString()
        assertTrue(result.contains("OR"))
    }

    @Test
    fun testStaticOfMethod() {
        val criteria = Criteria.of("name", OperatorEnum.EQ, "test")
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticAndWithCriterions() {
        val criteria = Criteria.and(
            Criterion("name", OperatorEnum.EQ, "test1"),
            Criterion("age", OperatorEnum.GT, 18)
        )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticAndWithCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test1")
        val criteria2 = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria.and(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticAndWithCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val nestedCriteria = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria.and(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticOrWithCriterions() {
        val criteria = Criteria.or(
            Criterion("name", OperatorEnum.EQ, "test1"),
            Criterion("name", OperatorEnum.EQ, "test2")
        )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticOrWithCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test1")
        val criteria2 = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria.or(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticOrWithCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test1")
        val nestedCriteria = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria.or(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testChainCalls() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "test")
            .addAnd("age", OperatorEnum.GT, 18)
            .addOr(
                Criterion("status", OperatorEnum.EQ, "active"),
                Criterion("status", OperatorEnum.EQ, "pending")
            )
            .addAnd("deleted", OperatorEnum.EQ, false)
        assertFalse(criteria.isEmpty())
    }

    // ============================================================
    // 内部结构验证：criterionGroups 中 AND / OR / 嵌套的实际表现
    // ============================================================

    @Test
    fun testAndStoresEachCriterionAsIndividualElement() {
        val criteria = Criteria()
            .addAnd("a", OperatorEnum.EQ, "1")
            .addAnd("b", OperatorEnum.GT, 2)
        val groups = criteria.getCriterionGroups()
        assertEquals(2, groups.size, "两次 addAnd 各产生一个独立元素")
        assertTrue(groups[0] is Criterion)
        assertTrue(groups[1] is Criterion)
        assertEquals("a", (groups[0] as Criterion).property)
        assertEquals("b", (groups[1] as Criterion).property)
    }

    @Test
    fun testAndVarargStoresMultipleCriterionsSeparately() {
        val criteria = Criteria().addAnd(
            Criterion("a", OperatorEnum.EQ, "1"),
            Criterion("b", OperatorEnum.EQ, "2"),
            Criterion("c", OperatorEnum.EQ, "3")
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(3, groups.size, "vararg AND 展开为多个独立 Criterion 元素")
        groups.forEach { assertTrue(it is Criterion) }
    }

    @Test
    fun testOrStoresGroupAsArrayWrapper() {
        val criteria = Criteria().addOr(
            Criterion("name", OperatorEnum.EQ, "a"),
            Criterion("name", OperatorEnum.EQ, "b")
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(1, groups.size, "addOr 整体只产生 1 个 group 元素")
        val group = groups[0]
        assertTrue(group is Array<*>, "OR group 在 criterionGroups 中表示为 Array<*>")
        assertEquals(2, group.size)
        assertTrue(group[0] is Criterion)
        assertTrue(group[1] is Criterion)
    }

    @Test
    fun testNestedCriteriaInAndStoredAsCriteriaNotArray() {
        val inner = Criteria("age", OperatorEnum.GT, 18)
        val outer = Criteria("name", OperatorEnum.EQ, "x").addAnd(inner)
        val groups = outer.getCriterionGroups()
        assertEquals(2, groups.size)
        assertTrue(groups[0] is Criterion)
        assertTrue(groups[1] is Criteria, "AND 嵌套保留为 Criteria，不会被 Array 包裹")
        assertSame(inner, groups[1], "保留的是同一个嵌套对象，不是拷贝")
    }

    @Test
    fun testAndOrAndMixedSequenceStructure() {
        val criteria = Criteria()
            .addAnd("status", OperatorEnum.EQ, "active")
            .addOr(
                Criterion("type", OperatorEnum.EQ, "A"),
                Criterion("type", OperatorEnum.EQ, "B")
            )
            .addAnd("deleted", OperatorEnum.EQ, false)
        val groups = criteria.getCriterionGroups()
        assertEquals(3, groups.size, "AND-OR-AND 各占一个 group 元素，保持加入顺序")
        assertTrue(groups[0] is Criterion)
        assertTrue(groups[1] is Array<*>)
        assertTrue(groups[2] is Criterion)
    }

    // ============================================================
    // 过滤行为：vararg 混合有效/无效条件
    // ============================================================

    @Test
    fun testAndVarargFiltersMixedValidAndInvalid() {
        val criteria = Criteria().addAnd(
            Criterion("a", OperatorEnum.EQ, "valid"),
            Criterion("b", OperatorEnum.EQ, ""),       // 空串：过滤
            Criterion("c", OperatorEnum.EQ, null),     // null + EQ 不接受 null：过滤
            Criterion("d", OperatorEnum.EQ, "also")    // 有效
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(2, groups.size, "只保留两个有效 criterion")
        assertEquals("a", (groups[0] as Criterion).property)
        assertEquals("d", (groups[1] as Criterion).property)
    }

    @Test
    fun testOrVarargFiltersInvalidPreservingValidOnes() {
        val criteria = Criteria().addOr(
            Criterion("a", OperatorEnum.EQ, "valid"),
            Criterion("b", OperatorEnum.EQ, ""),
            Criterion("c", OperatorEnum.EQ, null)
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(1, groups.size, "至少有 1 个有效 criterion 时仍产生 OR group")
        val orGroup = groups[0] as Array<*>
        assertEquals(1, orGroup.size)
        assertEquals("a", (orGroup[0] as Criterion).property)
    }

    @Test
    fun testOrWithAllCriterionsFilteredAddsNoGroup() {
        val criteria = Criteria().addOr(
            Criterion("a", OperatorEnum.EQ, ""),
            Criterion("b", OperatorEnum.EQ, null)
        )
        assertTrue(
            criteria.isEmpty(),
            "addOr 全部被过滤时不应留下空 OR group"
        )
    }

    @Test
    fun testOrWithAllEmptyNestedCriteriaAddsNoGroup() {
        val criteria = Criteria().addOr(Criteria(), Criteria())
        assertTrue(criteria.isEmpty(), "全是空嵌套 Criteria 的 addOr 也不留 group")
    }

    @Test
    fun testEmptyNestedCriteriaIsFilteredFromOrGroup() {
        val empty = Criteria()
        val nonEmpty = Criteria("a", OperatorEnum.EQ, "1")
        val criteria = Criteria().addOr(empty, nonEmpty)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(1, orGroup.size, "OR group 内的空嵌套也被剔除")
        assertSame(nonEmpty, orGroup[0])
    }

    @Test
    fun testAddAndEmptyVarargIsNoOp() {
        val criteria = Criteria("name", OperatorEnum.EQ, "x")
        val before = criteria.getCriterionGroups().size
        criteria.addAnd(*arrayOf<Criterion>())
        assertEquals(before, criteria.getCriterionGroups().size, "vararg 为空时不改动")
    }

    @Test
    fun testAddOrEmptyVarargIsNoOp() {
        val criteria = Criteria("name", OperatorEnum.EQ, "x")
        val before = criteria.getCriterionGroups().size
        criteria.addOr(*arrayOf<Criterion>())
        assertEquals(before, criteria.getCriterionGroups().size)
    }

    // ============================================================
    // 单值过滤的边界：哪些"看似空"的值实际会被保留
    // ============================================================

    @Test
    fun testWhitespaceOnlyStringIsKept() {
        // shouldAddCriterion 用的是 isNotEmpty 而非 isNotBlank：纯空白字符串被保留
        val criteria = Criteria().addAnd("name", OperatorEnum.EQ, "   ")
        assertFalse(criteria.isEmpty(), "纯空白字符串不被视为空——使用的是 isNotEmpty")
    }

    @Test
    fun testNumericZeroIsKept() {
        val criteria = Criteria().addAnd("count", OperatorEnum.EQ, 0)
        assertFalse(criteria.isEmpty(), "数值 0 不是空")
    }

    @Test
    fun testBooleanFalseIsKept() {
        val criteria = Criteria().addAnd("active", OperatorEnum.EQ, false)
        assertFalse(criteria.isEmpty(), "false 不是空")
    }

    @Test
    fun testEmptyPrimitiveIntArrayBypassesFilter() {
        // KNOWN BEHAVIOR：shouldAddCriterion 的 when 只匹配 Array<*>（即 Object[]），
        // 不匹配 IntArray/LongArray 等原始数组——它们走 else -> true，所以即使为空也保留。
        val criteria = Criteria().addAnd("ids", OperatorEnum.IN, intArrayOf())
        assertFalse(
            criteria.isEmpty(),
            "原始类型空数组（IntArray 等）不会被过滤——这是已知行为，不是 bug"
        )
    }

    @Test
    fun testEmptyMapBypassesFilter() {
        // KNOWN BEHAVIOR：Map 不在 shouldAddCriterion 的 when 里，走 else -> true
        val criteria = Criteria().addAnd("attrs", OperatorEnum.IN, emptyMap<String, String>())
        assertFalse(
            criteria.isEmpty(),
            "空 Map 不被过滤——shouldAddCriterion 未处理 Map 类型"
        )
    }

    // ============================================================
    // acceptNull 操作符（IS_NULL / IS_NOT_NULL / IS_EMPTY / IS_NOT_EMPTY）
    // ============================================================

    @Test
    fun testIsNullOperatorPassesWithAnyValue() {
        // acceptNull=true 的操作符直接放行
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NULL, null).isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NULL, "").isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NULL, emptyList<Int>()).isEmpty())
    }

    @Test
    fun testIsNotNullOperatorPassesWithAnyValue() {
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NOT_NULL, null).isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NOT_NULL, "").isEmpty())
    }

    @Test
    fun testIsEmptyOperatorPassesWithAnyValue() {
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_EMPTY, null).isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_EMPTY, "").isEmpty())
    }

    @Test
    fun testIsNotEmptyOperatorPassesWithAnyValue() {
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NOT_EMPTY, null).isEmpty())
    }

    // ============================================================
    // OR group 内 Criterion + Criteria 混合的顺序
    // ============================================================

    @Test
    fun testOrCriterionThenCriteriaPreservesOrder() {
        val criterion = Criterion("a", OperatorEnum.EQ, "1")
        val nested = Criteria("b", OperatorEnum.EQ, "2")
        val criteria = Criteria().addOr(criterion, nested)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(2, orGroup.size)
        assertTrue(orGroup[0] is Criterion, "顺序：criterion 先，criteria 后")
        assertTrue(orGroup[1] is Criteria)
    }

    @Test
    fun testOrCriteriaThenCriterionPreservesOrder() {
        val nested = Criteria("a", OperatorEnum.EQ, "1")
        val criterion = Criterion("b", OperatorEnum.EQ, "2")
        val criteria = Criteria().addOr(nested, criterion)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(2, orGroup.size)
        assertTrue(orGroup[0] is Criteria, "顺序：criteria 先，criterion 后")
        assertTrue(orGroup[1] is Criterion)
    }

    // ============================================================
    // toString 精确输出格式
    // ============================================================

    @Test
    fun testEmptyCriteriaToStringIsEmpty() {
        assertEquals("", Criteria().toString())
    }

    @Test
    fun testSingleCriterionToStringFormat() {
        val criteria = Criteria("name", OperatorEnum.EQ, "alice")
        assertEquals("name = alice", criteria.toString())
    }

    @Test
    fun testMultipleAndJoinedByAndKeyword() {
        val criteria = Criteria("name", OperatorEnum.EQ, "alice")
            .addAnd("age", OperatorEnum.GT, 18)
        assertEquals("name = alice AND age > 18", criteria.toString())
    }

    @Test
    fun testOrWrappedInParenthesesWithOrKeyword() {
        val criteria = Criteria().addOr(
            Criterion("status", OperatorEnum.EQ, "active"),
            Criterion("status", OperatorEnum.EQ, "pending")
        )
        assertEquals("(status = active OR status = pending)", criteria.toString())
    }

    @Test
    fun testCombinedAndOrToStringStructure() {
        val criteria = Criteria("flag", OperatorEnum.EQ, true)
            .addOr(
                Criterion("a", OperatorEnum.EQ, "1"),
                Criterion("b", OperatorEnum.EQ, "2")
            )
            .addAnd("c", OperatorEnum.EQ, "3")
        assertEquals(
            "flag = true AND (a = 1 OR b = 2) AND c = 3",
            criteria.toString()
        )
    }

    @Test
    fun testToStringRendersIsNullWithEmptyValue() {
        // Criterion.toString 会 trim 末尾空白，所以 IS NULL 的 null 值不显示
        val criteria = Criteria("x", OperatorEnum.IS_NULL, null)
        assertEquals("x IS NULL", criteria.toString())
    }

    // ============================================================
    // 链式调用返回值与引用一致性
    // ============================================================

    @Test
    fun testAddAndReturnsSameInstanceForChaining() {
        val criteria = Criteria()
        val returned = criteria.addAnd("a", OperatorEnum.EQ, "1")
        assertSame(criteria, returned, "addAnd 返回 this，支持链式")
    }

    @Test
    fun testAddOrReturnsSameInstanceForChaining() {
        val criteria = Criteria()
        val returned = criteria.addOr(Criterion("a", OperatorEnum.EQ, "1"))
        assertSame(criteria, returned, "addOr 返回 this，支持链式")
    }

    // ============================================================
    // 静态工厂未覆盖的重载
    // ============================================================

    @Test
    fun testStaticOrCriteriaAndCriterionStructure() {
        val nested = Criteria("a", OperatorEnum.EQ, "1")
        val criterion = Criterion("b", OperatorEnum.EQ, "2")
        val criteria = Criteria.or(nested, criterion)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(2, orGroup.size)
        assertTrue(orGroup[0] is Criteria)
        assertTrue(orGroup[1] is Criterion)
    }

    @Test
    fun testStaticAndCriteriaAndCriterionStructure() {
        val nested = Criteria("a", OperatorEnum.EQ, "1")
        val criterion = Criterion("b", OperatorEnum.EQ, "2")
        val criteria = Criteria.and(nested, criterion)
        val groups = criteria.getCriterionGroups()
        assertEquals(2, groups.size, "AND 不包 Array，两个元素各占一格")
        assertTrue(groups[0] is Criteria)
        assertTrue(groups[1] is Criterion)
    }

    // ============================================================
    // getCriterionGroups 是不可修改视图（防御封装泄漏）
    // ============================================================

    @Test
    fun testGetCriterionGroupsIsUnmodifiable() {
        val criteria = Criteria("a", OperatorEnum.EQ, "1")
        val groups = criteria.getCriterionGroups()
        @Suppress("UNCHECKED_CAST")
        val mutableView = groups as MutableList<Any>
        // 强转能编译过（List 在 JVM 下底层就是 java.util.List），但任何 mutation 应抛
        val mutationThrew = runCatching {
            mutableView.add(Criterion("b", OperatorEnum.EQ, "2"))
        }.exceptionOrNull() is UnsupportedOperationException
        assertTrue(mutationThrew, "getCriterionGroups 返回的视图应阻止 mutation")
    }

    @Test
    fun testGetCriterionGroupsViewReflectsLaterChanges() {
        // 是 view 不是 copy：之后再 addAnd，view 也能看到新元素
        val criteria = Criteria()
        val view = criteria.getCriterionGroups()
        assertEquals(0, view.size)
        criteria.addAnd("a", OperatorEnum.EQ, "1")
        assertEquals(1, view.size, "返回的应是 live view，不是 snapshot")
    }

    // ============================================================
    // 嵌套 Criteria 的多层组合
    // ============================================================

    @Test
    fun testDeeplyNestedCriteriaStructure() {
        val innermost = Criteria("c", OperatorEnum.EQ, "3")
        val middle = Criteria("b", OperatorEnum.EQ, "2").addAnd(innermost)
        val outer = Criteria("a", OperatorEnum.EQ, "1").addAnd(middle)
        val groups = outer.getCriterionGroups()
        assertEquals(2, groups.size)
        val nestedGroups = (groups[1] as Criteria).getCriterionGroups()
        assertEquals(2, nestedGroups.size)
        assertSame(innermost, nestedGroups[1])
    }
}
