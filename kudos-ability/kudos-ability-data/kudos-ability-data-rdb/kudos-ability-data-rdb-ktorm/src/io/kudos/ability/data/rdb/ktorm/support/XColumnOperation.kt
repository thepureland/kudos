package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.VarcharSqlType

/**
 * Ktorm [ColumnDeclaring] 上的"列 vs 列"或"列 vs 值"扩展操作集合。
 *
 * Ktorm 原生 DSL 提供 `eq` / `like` 等，但缺少：
 *  - 大小写不敏感的等值 / 模糊匹配（[ieq] / [ilike]，通过 `UPPER()` 实现）
 *  - 字符串列与字符串列 / 字符串值的大小比较（[columnGt] / [columnLt] / [columnGe] / [columnLe]）
 *
 * 之所以叫 `column*` 系列而不是直接重载 `>`，是因为 Ktorm 已经把 `greater` / `greaterEq`
 * 用于 `Comparable<*>` 列，与本处需要 `String` 列字典序比较的语义不同。
 *
 * 所有方法都是纯 Kotlin 扩展，无副作用、无状态。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */


/**
 * 大小写不敏感的 LIKE（列 vs 列）：`UPPER(left) LIKE right`。右侧表达式自身需已大写。
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
 * 大小写不敏感的 LIKE（列 vs 字面值）：`UPPER(left) LIKE UPPER(value)`。
 * 字面值会通过 [String.uppercase] 转大写后参与匹配。
 */
infix fun ColumnDeclaring<*>.ilike(value: String): BinaryExpression<Boolean> {
    return this ilike ArgumentExpression(value.uppercase(), VarcharSqlType)
}

/**
 * 大小写不敏感的等值（列 vs 列）：`UPPER(left) = right`。右侧表达式自身需已大写。
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
 * 大小写不敏感的等值（列 vs 字面值）：`UPPER(left) = UPPER(value)`。
 * 字面值会通过 [String.uppercase] 转大写后比较。
 */
infix fun ColumnDeclaring<*>.ieq(value: String): BinaryExpression<Boolean> {
    return this ieq ArgumentExpression(value.uppercase(), VarcharSqlType)
}

/**
 * 字符串列大于字符串列：`left > right`（按 DB 字典序）。
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
 * 字符串列大于字符串字面值：`left > value`。
 */
infix fun ColumnDeclaring<*>.columnGt(value: String): BinaryExpression<Boolean> {
    return this columnGt ArgumentExpression(value, VarcharSqlType)
}

/**
 * 字符串列小于字符串列：`left < right`。
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
 * 字符串列小于字符串字面值：`left < value`。
 */
infix fun ColumnDeclaring<*>.columnLt(value: String): BinaryExpression<Boolean> {
    return this columnLt ArgumentExpression(value, VarcharSqlType)
}

/**
 * 字符串列大于等于字符串列：`left >= right`。
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
 * 字符串列大于等于字符串字面值：`left >= value`。
 */
infix fun ColumnDeclaring<*>.columnGe(value: String): BinaryExpression<Boolean> {
    return this columnGe ArgumentExpression(value, VarcharSqlType)
}

/**
 * 字符串列小于等于字符串列：`left <= right`。
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
 * 字符串列小于等于字符串字面值：`left <= value`。
 */
infix fun ColumnDeclaring<*>.columnLe(value: String): BinaryExpression<Boolean> {
    return this columnLe ArgumentExpression(value, VarcharSqlType)
}
