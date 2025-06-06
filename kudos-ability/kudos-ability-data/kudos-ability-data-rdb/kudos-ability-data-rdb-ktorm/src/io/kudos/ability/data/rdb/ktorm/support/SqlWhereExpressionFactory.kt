package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.Consts
import org.ktorm.dsl.*
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring

/**
 * 查询条件表达式工厂
 *
 * @author K
 * @since 1.0.0
 */
object SqlWhereExpressionFactory {

    /**
     * 创建查询条件表达式
     *
     * @param column 列对象
     * @param operator 操作符
     * @param value 要查询的值
     * @return 列申明对象
     * @author K
     * @since 1.0.0
     */
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    fun create(column: Column<Any>, operator: OperatorEnum, value: Any?): ColumnDeclaring<Boolean>? {
        if (value == null && operator !in arrayOf(OperatorEnum.IS_NULL, OperatorEnum.IS_NOT_NULL)) {
            return null
        }
        return when (operator) {
            OperatorEnum.EQ -> column.eq(value!!)
            OperatorEnum.NE, OperatorEnum.LG -> column.notEq(value!!)
            OperatorEnum.GT -> (column as Column<Comparable<Any>>).greater(value as Comparable<Any>)
            OperatorEnum.GE -> (column as Column<Comparable<Any>>).greaterEq(value as Comparable<Any>)
            OperatorEnum.LT -> (column as Column<Comparable<Any>>).less(value as Comparable<Any>)
            OperatorEnum.LE -> (column as Column<Comparable<Any>>).lessEq(value as Comparable<Any>)
            OperatorEnum.IEQ -> column.ieq(value.toString().uppercase())
            OperatorEnum.EQ_P -> columnEq(
                column, ColumnHelper.columnOf(column.table, value as String)[value] as Column<Any>
            )
            OperatorEnum.NE_P -> columnNotEq(
                column, ColumnHelper.columnOf(column.table, value as String)[value] as Column<Any>
            )
            OperatorEnum.GE_P -> column.columnGe(
                ColumnHelper.columnOf(column.table, value as String)[value] as Column<String>
            )
            OperatorEnum.LE_P -> column.columnLe(
                ColumnHelper.columnOf(column.table, value as String)[value] as Column<String>
            )
            OperatorEnum.GT_P -> column.columnGt(
                ColumnHelper.columnOf(column.table, value as String)[value] as Column<String>
            )
            OperatorEnum.LT_P -> column.columnLt(
                ColumnHelper.columnOf(column.table, value as String)[value] as Column<String>
            )
            OperatorEnum.LIKE -> column.like("%${value!!}%")
            OperatorEnum.LIKE_S -> column.like("${value!!}%")
            OperatorEnum.LIKE_E -> column.like("%${value!!}")
            OperatorEnum.ILIKE -> column.ilike("%${value!!}%")
            OperatorEnum.ILIKE_S -> column.ilike("${value!!}%")
            OperatorEnum.ILIKE_E -> column.ilike("%${value!!}")
            OperatorEnum.IN -> handleIn(true, value!!, column)
            OperatorEnum.NOT_IN -> handleIn(false, value!!, column)
            OperatorEnum.IS_NULL -> column.isNull()
            OperatorEnum.IS_NOT_NULL -> column.isNotNull()
            OperatorEnum.IS_EMPTY -> column.eq("")
            OperatorEnum.IS_NOT_EMPTY -> column.notEq("")
            OperatorEnum.BETWEEN -> (column as Column<Comparable<Any>>).between(value as ClosedRange<Comparable<Any>>)
            OperatorEnum.NOT_BETWEEN -> (column as Column<Comparable<Any>>).notBetween(value as ClosedRange<Comparable<Any>>)
            else -> error("未支持")
        }
    }

    // 为了解决 <T : Any> ColumnDeclaring<T>.eq(expr: ColumnDeclaring<T>) 的泛型问题
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    private fun <T : Any> columnEq(
        column: ColumnDeclaring<T>, anotherColumn: Column<Any>
    ): ColumnDeclaring<Boolean> =
        column.eq(anotherColumn as Column<T>)

    // 为了解决 <T : Any> ColumnDeclaring<T>.notEq(expr: ColumnDeclaring<T>) 的泛型问题
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    private fun <T : Any> columnNotEq(
        column: ColumnDeclaring<T>, anotherColumn: Column<*>
    ): ColumnDeclaring<Boolean> =
        column.notEq(anotherColumn as Column<T>)

    // 为了解决 <T : Any> ColumnDeclaring<T>.inList(list: Collection<T>) 的泛型问题
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    private fun <T : Any> columnIn(column: ColumnDeclaring<T>, values: Collection<T>): ColumnDeclaring<Boolean> =
        column.inList(values)

    // 为了解决 <T : Any> ColumnDeclaring<T>.notInList(list: Collection<T>) 的泛型问题
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    private fun <T : Any> columnNotIn(column: ColumnDeclaring<T>, values: Collection<T>): ColumnDeclaring<Boolean> =
        column.notInList(values)

    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    private fun handleIn(isIn: Boolean, value: Any, column: ColumnDeclaring<Any>): ColumnDeclaring<Boolean> {
        var values = value
        if (values !is Collection<*> && values !is Array<*>) {
            values = arrayOf(values)
        }
        if (values is Array<*>) {
            values = listOf(*values)
        }
        return if (isIn) {
            columnIn(column, values as Collection<Any>)
        } else {
            columnNotIn(column, values as Collection<Any>)
        }
    }

}