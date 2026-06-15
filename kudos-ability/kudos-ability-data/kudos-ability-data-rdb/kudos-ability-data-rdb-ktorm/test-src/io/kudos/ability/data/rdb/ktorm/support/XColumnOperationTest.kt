package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.VarcharSqlType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for the [org.ktorm.schema.ColumnDeclaring] extension operators in XColumnOperation.kt.
 *
 * Each operator has two overloads (column-vs-column expression and column-vs-literal); both are
 * asserted structurally: expression type, the `UPPER()` wrapping for the case-insensitive variants
 * and the literal uppercasing rule ([ilike]/[ieq] uppercase the literal, the `column*` ordering
 * comparisons must NOT alter the literal). Pure expression construction — no database involved.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class XColumnOperationTest {

    private val name = TestTableKtorms.name

    @Test
    fun ilike_literal_uppercasesValueAndWrapsColumnWithUpper() {
        val expr = name ilike "aBc%"
        assertEquals(BinaryExpressionType.LIKE, expr.type)
        val left = assertIs<FunctionExpression<*>>(expr.left)
        assertEquals("upper", left.functionName)
        val right = assertIs<ArgumentExpression<*>>(expr.right)
        assertEquals("ABC%", right.value, "ilike must uppercase the literal so it matches UPPER(column)")
    }

    @Test
    fun ilike_expression_usesRightExpressionAsIs() {
        val expr = name ilike ArgumentExpression("ALREADY_UPPER%", VarcharSqlType)
        assertEquals(BinaryExpressionType.LIKE, expr.type)
        assertEquals("ALREADY_UPPER%", (expr.right as ArgumentExpression<*>).value)
    }

    @Test
    fun ieq_literal_uppercasesValueAndWrapsColumnWithUpper() {
        val expr = name ieq "MiXeD"
        assertEquals(BinaryExpressionType.EQUAL, expr.type)
        val left = assertIs<FunctionExpression<*>>(expr.left)
        assertEquals("upper", left.functionName)
        assertEquals("MIXED", (expr.right as ArgumentExpression<*>).value)
    }

    @Test
    fun ieq_expression_usesRightExpressionAsIs() {
        val expr = name ieq ArgumentExpression("UP", VarcharSqlType)
        assertEquals(BinaryExpressionType.EQUAL, expr.type)
        assertEquals("UP", (expr.right as ArgumentExpression<*>).value)
    }

    @Test
    fun columnGt_literalAndExpression() {
        val literal = name columnGt "m"
        assertEquals(BinaryExpressionType.GREATER_THAN, literal.type)
        assertEquals("m", (literal.right as ArgumentExpression<*>).value, "ordering comparisons must not uppercase the literal")

        val expr = name columnGt ArgumentExpression("m", VarcharSqlType)
        assertEquals(BinaryExpressionType.GREATER_THAN, expr.type)
    }

    @Test
    fun columnLt_literalAndExpression() {
        val literal = name columnLt "m"
        assertEquals(BinaryExpressionType.LESS_THAN, literal.type)
        assertEquals("m", (literal.right as ArgumentExpression<*>).value)

        val expr = name columnLt ArgumentExpression("m", VarcharSqlType)
        assertEquals(BinaryExpressionType.LESS_THAN, expr.type)
    }

    @Test
    fun columnGe_literalAndExpression() {
        val literal = name columnGe "m"
        assertEquals(BinaryExpressionType.GREATER_THAN_OR_EQUAL, literal.type)
        assertEquals("m", (literal.right as ArgumentExpression<*>).value)

        val expr = name columnGe ArgumentExpression("m", VarcharSqlType)
        assertEquals(BinaryExpressionType.GREATER_THAN_OR_EQUAL, expr.type)
    }

    @Test
    fun columnLe_literalAndExpression() {
        val literal = name columnLe "m"
        assertEquals(BinaryExpressionType.LESS_THAN_OR_EQUAL, literal.type)
        assertEquals("m", (literal.right as ArgumentExpression<*>).value)

        val expr = name columnLe ArgumentExpression("m", VarcharSqlType)
        assertEquals(BinaryExpressionType.LESS_THAN_OR_EQUAL, expr.type)
    }
}
