package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import kotlin.reflect.KProperty1

/**
 * Criterion 条件表示式 DSL
 *
 * 允许通过属性引用构建查询条件，例如：SysResource::subSystemCode eq subSystemCode
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
private fun KProperty1<*, *>.op(operator: OperatorEnum, value: Any? = null): Criterion =
    Criterion(this.name, operator, value)

infix fun KProperty1<*, *>.eq(value: Any?): Criterion = op(OperatorEnum.EQ, value)
infix fun KProperty1<*, *>.ieq(value: String): Criterion = op(OperatorEnum.IEQ, value)
infix fun KProperty1<*, *>.ne(value: Any?): Criterion = op(OperatorEnum.NE, value)
infix fun KProperty1<*, *>.lg(value: Any?): Criterion = op(OperatorEnum.LG, value)
infix fun KProperty1<*, *>.ge(value: Any?): Criterion = op(OperatorEnum.GE, value)
infix fun KProperty1<*, *>.le(value: Any?): Criterion = op(OperatorEnum.LE, value)
infix fun KProperty1<*, *>.gt(value: Any?): Criterion = op(OperatorEnum.GT, value)
infix fun KProperty1<*, *>.lt(value: Any?): Criterion = op(OperatorEnum.LT, value)

infix fun KProperty1<*, *>.eqP(another: KProperty1<*, *>): Criterion = op(OperatorEnum.EQ_P, another.name)
infix fun KProperty1<*, *>.eqP(anotherPropertyName: String): Criterion = op(OperatorEnum.EQ_P, anotherPropertyName)
infix fun KProperty1<*, *>.neP(another: KProperty1<*, *>): Criterion = op(OperatorEnum.NE_P, another.name)
infix fun KProperty1<*, *>.neP(anotherPropertyName: String): Criterion = op(OperatorEnum.NE_P, anotherPropertyName)
infix fun KProperty1<*, *>.geP(another: KProperty1<*, *>): Criterion = op(OperatorEnum.GE_P, another.name)
infix fun KProperty1<*, *>.geP(anotherPropertyName: String): Criterion = op(OperatorEnum.GE_P, anotherPropertyName)
infix fun KProperty1<*, *>.leP(another: KProperty1<*, *>): Criterion = op(OperatorEnum.LE_P, another.name)
infix fun KProperty1<*, *>.leP(anotherPropertyName: String): Criterion = op(OperatorEnum.LE_P, anotherPropertyName)
infix fun KProperty1<*, *>.gtP(another: KProperty1<*, *>): Criterion = op(OperatorEnum.GT_P, another.name)
infix fun KProperty1<*, *>.gtP(anotherPropertyName: String): Criterion = op(OperatorEnum.GT_P, anotherPropertyName)
infix fun KProperty1<*, *>.ltP(another: KProperty1<*, *>): Criterion = op(OperatorEnum.LT_P, another.name)
infix fun KProperty1<*, *>.ltP(anotherPropertyName: String): Criterion = op(OperatorEnum.LT_P, anotherPropertyName)

infix fun KProperty1<*, *>.like(value: String): Criterion = op(OperatorEnum.LIKE, value)
infix fun KProperty1<*, *>.likeS(value: String): Criterion = op(OperatorEnum.LIKE_S, value)
infix fun KProperty1<*, *>.likeE(value: String): Criterion = op(OperatorEnum.LIKE_E, value)
infix fun KProperty1<*, *>.ilike(value: String): Criterion = op(OperatorEnum.ILIKE, value)
infix fun KProperty1<*, *>.ilikeS(value: String): Criterion = op(OperatorEnum.ILIKE_S, value)
infix fun KProperty1<*, *>.ilikeE(value: String): Criterion = op(OperatorEnum.ILIKE_E, value)

infix fun KProperty1<*, *>.inList(values: Collection<*>): Criterion = op(OperatorEnum.IN, values)
infix fun KProperty1<*, *>.inArray(values: Array<*>): Criterion = op(OperatorEnum.IN, values)
infix fun KProperty1<*, *>.notInList(values: Collection<*>): Criterion = op(OperatorEnum.NOT_IN, values)
infix fun KProperty1<*, *>.notInArray(values: Array<*>): Criterion = op(OperatorEnum.NOT_IN, values)

fun KProperty1<*, *>.isNull(): Criterion = op(OperatorEnum.IS_NULL)
fun KProperty1<*, *>.isNotNull(): Criterion = op(OperatorEnum.IS_NOT_NULL)
fun KProperty1<*, *>.isEmpty(): Criterion = op(OperatorEnum.IS_EMPTY, "")
fun KProperty1<*, *>.isNotEmpty(): Criterion = op(OperatorEnum.IS_NOT_EMPTY, "")

infix fun KProperty1<*, *>.between(range: ClosedRange<*>): Criterion = op(OperatorEnum.BETWEEN, range)
infix fun KProperty1<*, *>.notBetween(range: ClosedRange<*>): Criterion = op(OperatorEnum.NOT_BETWEEN, range)
