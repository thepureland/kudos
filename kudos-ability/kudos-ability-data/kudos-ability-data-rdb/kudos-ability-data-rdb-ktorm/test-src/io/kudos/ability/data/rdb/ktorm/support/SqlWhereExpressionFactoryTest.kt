package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import io.kudos.base.query.enums.OperatorEnum
import org.ktorm.expression.BetweenExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.InListExpression
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.Column
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SqlWhereExpressionFactory.create].
 *
 * Covers every [OperatorEnum] branch (including the column-vs-column *_P operators, the
 * IS_EMPTY / IS_NOT_EMPTY string shortcuts, BETWEEN / NOT_BETWEEN ranges and the IN / NOT_IN value
 * normalization for single values, arrays and collections), plus the "null value with a
 * non-nullable operator returns null" short circuit. Expressions are inspected structurally —
 * no database is required because the factory is pure expression-tree construction.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SqlWhereExpressionFactoryTest {

    @Suppress("UNCHECKED_CAST")
    private val nameColumn = TestTableKtorms.name as Column<Any>

    @Suppress("UNCHECKED_CAST")
    private val heightColumn = TestTableKtorms.height as Column<Any>

    @Test
    fun nullValue_withNonNullableOperator_returnsNull() {
        // null values are silently dropped for operators that cannot accept null —
        // the caller is expected to skip the condition entirely.
        assertNull(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.EQ, null))
        assertNull(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.LIKE, null))
        assertNull(SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.GT, null))
        assertNull(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IN, null))
    }

    @Test
    fun nullValue_withNullableOperator_stillBuildsExpression() {
        val isNull = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IS_NULL, null)
        assertIs<UnaryExpression<*>>(isNull)
        assertEquals(UnaryExpressionType.IS_NULL, (isNull as UnaryExpression<*>).type)

        val isNotNull = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IS_NOT_NULL, null)
        assertEquals(UnaryExpressionType.IS_NOT_NULL, (isNotNull as UnaryExpression<*>).type)
    }

    @Test
    fun equalityOperators() {
        val eq = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.EQ, "x")
        assertEquals(BinaryExpressionType.EQUAL, (eq as BinaryExpression<*>).type)

        val ne = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.NE, "x")
        assertEquals(BinaryExpressionType.NOT_EQUAL, (ne as BinaryExpression<*>).type)

        val lg = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.LG, "x")
        assertEquals(BinaryExpressionType.NOT_EQUAL, (lg as BinaryExpression<*>).type)

        // IEQ: UPPER(column) = UPPER(value)
        val ieq = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IEQ, "aBc")
        assertEquals(BinaryExpressionType.EQUAL, (ieq as BinaryExpression<*>).type)
    }

    @Test
    fun comparisonOperators() {
        assertEquals(
            BinaryExpressionType.GREATER_THAN,
            (SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.GT, 160) as BinaryExpression<*>).type
        )
        assertEquals(
            BinaryExpressionType.GREATER_THAN_OR_EQUAL,
            (SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.GE, 160) as BinaryExpression<*>).type
        )
        assertEquals(
            BinaryExpressionType.LESS_THAN,
            (SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.LT, 160) as BinaryExpression<*>).type
        )
        assertEquals(
            BinaryExpressionType.LESS_THAN_OR_EQUAL,
            (SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.LE, 160) as BinaryExpression<*>).type
        )
    }

    @Test
    fun columnVsColumnOperators() {
        // value is the *property name* of the other column on the same table
        val eqP = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.EQ_P, "weight")
        assertEquals(BinaryExpressionType.EQUAL, (eqP as BinaryExpression<*>).type)

        val neP = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.NE_P, "weight")
        assertEquals(BinaryExpressionType.NOT_EQUAL, (neP as BinaryExpression<*>).type)

        val geP = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.GE_P, "weight")
        assertEquals(BinaryExpressionType.GREATER_THAN_OR_EQUAL, (geP as BinaryExpression<*>).type)

        val leP = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.LE_P, "weight")
        assertEquals(BinaryExpressionType.LESS_THAN_OR_EQUAL, (leP as BinaryExpression<*>).type)

        val gtP = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.GT_P, "weight")
        assertEquals(BinaryExpressionType.GREATER_THAN, (gtP as BinaryExpression<*>).type)

        val ltP = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.LT_P, "weight")
        assertEquals(BinaryExpressionType.LESS_THAN, (ltP as BinaryExpression<*>).type)
    }

    @Test
    fun likeOperators_wrapValueWithExpectedWildcards() {
        fun rightArgOf(expr: Any?): Any? {
            val right = (expr as BinaryExpression<*>).right
            return (right as org.ktorm.expression.ArgumentExpression<*>).value
        }

        assertEquals("%v%", rightArgOf(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.LIKE, "v")))
        assertEquals("v%", rightArgOf(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.LIKE_S, "v")))
        assertEquals("%v", rightArgOf(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.LIKE_E, "v")))

        // ILIKE variants uppercase the literal
        assertEquals("%V%", rightArgOf(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.ILIKE, "v")))
        assertEquals("V%", rightArgOf(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.ILIKE_S, "v")))
        assertEquals("%V", rightArgOf(SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.ILIKE_E, "v")))
    }

    @Test
    fun inOperator_normalizesSingleValueArrayAndCollection() {
        // single non-collection value is wrapped into a one-element list
        val single = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IN, "only")
        assertIs<InListExpression>(single)
        assertEquals(1, (single as InListExpression).values?.size)
        assertEquals(false, single.notInList)

        // array is converted to a list
        val fromArray = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.IN, arrayOf(1, 2, 3))
        assertEquals(3, (fromArray as InListExpression).values?.size)

        // collection is used as-is
        val fromCollection = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.IN, listOf(7, 8))
        assertEquals(2, (fromCollection as InListExpression).values?.size)
    }

    @Test
    fun notInOperator_setsNotInListFlag() {
        val notIn = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.NOT_IN, listOf(1, 2))
        assertIs<InListExpression>(notIn)
        assertTrue((notIn as InListExpression).notInList)

        // single value goes through the same normalization
        val notInSingle = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.NOT_IN, "x")
        assertTrue((notInSingle as InListExpression).notInList)
    }

    @Test
    fun emptyStringOperators() {
        val isEmpty = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IS_EMPTY, "ignored")
        assertEquals(BinaryExpressionType.EQUAL, (isEmpty as BinaryExpression<*>).type)
        assertEquals("", ((isEmpty.right) as org.ktorm.expression.ArgumentExpression<*>).value)

        val isNotEmpty = SqlWhereExpressionFactory.create(nameColumn, OperatorEnum.IS_NOT_EMPTY, "ignored")
        assertEquals(BinaryExpressionType.NOT_EQUAL, (isNotEmpty as BinaryExpression<*>).type)
        assertEquals("", ((isNotEmpty.right) as org.ktorm.expression.ArgumentExpression<*>).value)
    }

    @Test
    fun betweenOperators() {
        val between = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.BETWEEN, 100..200)
        assertIs<BetweenExpression>(between)
        assertEquals(false, (between as BetweenExpression).notBetween)

        val notBetween = SqlWhereExpressionFactory.create(heightColumn, OperatorEnum.NOT_BETWEEN, 100..200)
        assertTrue((notBetween as BetweenExpression).notBetween)
    }

    @Test
    fun everyOperator_withValidValue_neverReturnsNull() {
        // Regression net: any new OperatorEnum entry must be handled by the factory (the when is
        // exhaustive at compile time, but this documents the runtime contract for valid inputs).
        val valueByOperator = mapOf(
            OperatorEnum.EQ to "v", OperatorEnum.NE to "v", OperatorEnum.LG to "v",
            OperatorEnum.GT to 1, OperatorEnum.GE to 1, OperatorEnum.LT to 1, OperatorEnum.LE to 1,
            OperatorEnum.IEQ to "v",
            OperatorEnum.EQ_P to "weight", OperatorEnum.NE_P to "weight",
            OperatorEnum.GE_P to "weight", OperatorEnum.LE_P to "weight",
            OperatorEnum.GT_P to "weight", OperatorEnum.LT_P to "weight",
            OperatorEnum.LIKE to "v", OperatorEnum.LIKE_S to "v", OperatorEnum.LIKE_E to "v",
            OperatorEnum.ILIKE to "v", OperatorEnum.ILIKE_S to "v", OperatorEnum.ILIKE_E to "v",
            OperatorEnum.IN to listOf("v"), OperatorEnum.NOT_IN to listOf("v"),
            OperatorEnum.IS_NULL to null, OperatorEnum.IS_NOT_NULL to null,
            OperatorEnum.IS_EMPTY to "v", OperatorEnum.IS_NOT_EMPTY to "v",
            OperatorEnum.BETWEEN to 1..2, OperatorEnum.NOT_BETWEEN to 1..2,
        )
        valueByOperator.forEach { (operator, value) ->
            val col = when (operator) {
                OperatorEnum.LIKE, OperatorEnum.LIKE_S, OperatorEnum.LIKE_E,
                OperatorEnum.ILIKE, OperatorEnum.ILIKE_S, OperatorEnum.ILIKE_E,
                OperatorEnum.IEQ, OperatorEnum.IS_EMPTY, OperatorEnum.IS_NOT_EMPTY -> nameColumn
                else -> heightColumn
            }
            assertNotNull(
                SqlWhereExpressionFactory.create(col, operator, value),
                "operator [$operator] must produce a non-null expression for a valid value"
            )
        }
    }
}
