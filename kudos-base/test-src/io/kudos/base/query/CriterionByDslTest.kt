package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Criterion DSL测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class CriterionByDslTest {

    private data class TestEntity(
        val name: String = "",
        val age: Int = 0,
        val score: Double = 0.0,
        val active: Boolean = false,
        val tag: String = "",
        val category: String = "",
        val level: Int = 0,
        val rank: Int = 0
    )

    @Test
    fun testEqDsl() {
        val criterion = TestEntity::name eq "test"
        assertEquals("name", criterion.property)
        assertEquals(OperatorEnum.EQ, criterion.operator)
        assertEquals("test", criterion.value)
        assertEquals(null, criterion.alias)
        assertFalse(criterion.encrypt)
    }

    @Test
    fun testAllOperatorDsl() {
        assertEquals(OperatorEnum.EQ, (TestEntity::name eq "n").operator)
        assertEquals(OperatorEnum.IEQ, (TestEntity::name ieq "n").operator)
        assertEquals(OperatorEnum.NE, (TestEntity::name ne "n").operator)
        assertEquals(OperatorEnum.LG, (TestEntity::name lg "n").operator)
        assertEquals(OperatorEnum.GE, (TestEntity::age ge 10).operator)
        assertEquals(OperatorEnum.LE, (TestEntity::age le 10).operator)
        assertEquals(OperatorEnum.GT, (TestEntity::age gt 10).operator)
        assertEquals(OperatorEnum.LT, (TestEntity::age lt 10).operator)

        assertEquals(OperatorEnum.EQ_P, (TestEntity::name eqP TestEntity::tag).operator)
        assertEquals("tag", (TestEntity::name eqP TestEntity::tag).value)
        assertEquals(OperatorEnum.NE_P, (TestEntity::name neP "category").operator)
        assertEquals("category", (TestEntity::name neP "category").value)
        assertEquals(OperatorEnum.GE_P, (TestEntity::level geP TestEntity::rank).operator)
        assertEquals(OperatorEnum.LE_P, (TestEntity::level leP "rank").operator)
        assertEquals(OperatorEnum.GT_P, (TestEntity::level gtP TestEntity::rank).operator)
        assertEquals(OperatorEnum.LT_P, (TestEntity::level ltP "rank").operator)

        assertEquals(OperatorEnum.LIKE, (TestEntity::name like "abc").operator)
        assertEquals(OperatorEnum.LIKE_S, (TestEntity::name likeS "abc").operator)
        assertEquals(OperatorEnum.LIKE_E, (TestEntity::name likeE "abc").operator)
        assertEquals(OperatorEnum.ILIKE, (TestEntity::name ilike "abc").operator)
        assertEquals(OperatorEnum.ILIKE_S, (TestEntity::name ilikeS "abc").operator)
        assertEquals(OperatorEnum.ILIKE_E, (TestEntity::name ilikeE "abc").operator)

        val inListCriterion = TestEntity::age inList listOf(1, 2, 3)
        assertEquals(OperatorEnum.IN, inListCriterion.operator)
        assertEquals(listOf(1, 2, 3), inListCriterion.value)

        val inArrayCriterion = TestEntity::age inArray arrayOf(1, 2, 3)
        assertEquals(OperatorEnum.IN, inArrayCriterion.operator)
        assertTrue((inArrayCriterion.value as Array<*>).contentEquals(arrayOf(1, 2, 3)))

        val notInListCriterion = TestEntity::age notInList listOf(1, 2, 3)
        assertEquals(OperatorEnum.NOT_IN, notInListCriterion.operator)
        assertEquals(listOf(1, 2, 3), notInListCriterion.value)

        val notInArrayCriterion = TestEntity::age notInArray arrayOf(1, 2, 3)
        assertEquals(OperatorEnum.NOT_IN, notInArrayCriterion.operator)
        assertTrue((notInArrayCriterion.value as Array<*>).contentEquals(arrayOf(1, 2, 3)))

        assertEquals(OperatorEnum.IS_NULL, TestEntity::name.isNull().operator)
        assertEquals(OperatorEnum.IS_NOT_NULL, TestEntity::name.isNotNull().operator)
        assertEquals(OperatorEnum.IS_EMPTY, TestEntity::name.isEmpty().operator)
        assertEquals("", TestEntity::name.isEmpty().value)
        assertEquals(OperatorEnum.IS_NOT_EMPTY, TestEntity::name.isNotEmpty().operator)
        assertEquals("", TestEntity::name.isNotEmpty().value)

        val betweenCriterion = TestEntity::age between (1..10)
        assertEquals(OperatorEnum.BETWEEN, betweenCriterion.operator)
        assertEquals(1..10, betweenCriterion.value)

        val notBetweenCriterion = TestEntity::age notBetween (1..10)
        assertEquals(OperatorEnum.NOT_BETWEEN, notBetweenCriterion.operator)
        assertEquals(1..10, notBetweenCriterion.value)
    }
}
