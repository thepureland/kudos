package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import kotlin.test.Test
import kotlin.test.assertFalse
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
}
