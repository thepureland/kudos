package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.VarcharSqlType

/**
 * Extension operators on Ktorm [ColumnDeclaring] for "column vs column" or "column vs value" comparisons.
 *
 * Ktorm's native DSL provides `eq` / `like`, but lacks:
 *  - Case-insensitive equality / fuzzy match ([ieq] / [ilike], implemented via `UPPER()`).
 *  - String-column vs string-column / string-value ordering comparisons
 *    ([columnGt] / [columnLt] / [columnGe] / [columnLe]).
 *
 * These are named with a `column*` prefix rather than overloading `>` because Ktorm already uses
 * `greater` / `greaterEq` for `Comparable<*>` columns, which has different semantics from the
 * lexicographic comparison on `String` columns required here.
 *
 * All methods are pure Kotlin extensions — side-effect-free and stateless.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */


/**
 * Case-insensitive LIKE (column vs column): `UPPER(left) LIKE right`. The right expression must already be uppercased.
 */
infix fun ColumnDeclaring<*>.ilike(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(
        BinaryExpressionType.LIKE,
        FunctionExpression("upper", listOf(asExpression()), VarcharSqlType),
        expr.asExpression(),
        BooleanSqlType
    )
}

/**
 * Case-insensitive LIKE (column vs literal): `UPPER(left) LIKE UPPER(value)`.
 * The literal is uppercased via [String.uppercase] before matching.
 */
infix fun ColumnDeclaring<*>.ilike(value: String): BinaryExpression<Boolean> {
    return this ilike ArgumentExpression(value.uppercase(), VarcharSqlType)
}

/**
 * Case-insensitive equality (column vs column): `UPPER(left) = right`. The right expression must already be uppercased.
 */
infix fun ColumnDeclaring<*>.ieq(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(
        BinaryExpressionType.EQUAL,
        FunctionExpression("upper", listOf(asExpression()), VarcharSqlType),
        expr.asExpression(),
        BooleanSqlType
    )
}

/**
 * Case-insensitive equality (column vs literal): `UPPER(left) = UPPER(value)`.
 * The literal is uppercased via [String.uppercase] before comparison.
 */
infix fun ColumnDeclaring<*>.ieq(value: String): BinaryExpression<Boolean> {
    return this ieq ArgumentExpression(value.uppercase(), VarcharSqlType)
}

/**
 * String column greater than string column: `left > right` (DB lexicographic order).
 */
infix fun ColumnDeclaring<*>.columnGt(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(
        BinaryExpressionType.GREATER_THAN,
        asExpression(),
        expr.asExpression(),
        BooleanSqlType
    )
}

/**
 * String column greater than string literal: `left > value`.
 */
infix fun ColumnDeclaring<*>.columnGt(value: String): BinaryExpression<Boolean> {
    return this columnGt ArgumentExpression(value, VarcharSqlType)
}

/**
 * String column less than string column: `left < right`.
 */
infix fun ColumnDeclaring<*>.columnLt(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(
        BinaryExpressionType.LESS_THAN,
        asExpression(),
        expr.asExpression(),
        BooleanSqlType
    )
}

/**
 * String column less than string literal: `left < value`.
 */
infix fun ColumnDeclaring<*>.columnLt(value: String): BinaryExpression<Boolean> {
    return this columnLt ArgumentExpression(value, VarcharSqlType)
}

/**
 * String column greater than or equal to string column: `left >= right`.
 */
infix fun ColumnDeclaring<*>.columnGe(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(
        BinaryExpressionType.GREATER_THAN_OR_EQUAL,
        asExpression(),
        expr.asExpression(),
        BooleanSqlType
    )
}

/**
 * String column greater than or equal to string literal: `left >= value`.
 */
infix fun ColumnDeclaring<*>.columnGe(value: String): BinaryExpression<Boolean> {
    return this columnGe ArgumentExpression(value, VarcharSqlType)
}

/**
 * String column less than or equal to string column: `left <= right`.
 */
infix fun ColumnDeclaring<*>.columnLe(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(
        BinaryExpressionType.LESS_THAN_OR_EQUAL,
        asExpression(),
        expr.asExpression(),
        BooleanSqlType
    )
}

/**
 * String column less than or equal to string literal: `left <= value`.
 */
infix fun ColumnDeclaring<*>.columnLe(value: String): BinaryExpression<Boolean> {
    return this columnLe ArgumentExpression(value, VarcharSqlType)
}
