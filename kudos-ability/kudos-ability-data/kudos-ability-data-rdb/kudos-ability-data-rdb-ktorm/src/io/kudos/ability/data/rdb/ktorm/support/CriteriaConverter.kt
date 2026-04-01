package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import org.ktorm.dsl.and
import org.ktorm.dsl.or
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * Criteria转换器，可将Criteria转换为Ktorm查询条件表达式
 *
 * @author K
 * @since 1.0.0
 */
internal object CriteriaConverter {

    /**
     * 将Criteria转换为Ktorm的表达式
     *
     * @param criteria Criteria
     * @param table ktorm表对象
     * @return Ktorm查询条件的表达式
     * @author K
     * @since 1.0.0
     */
    fun convert(criteria: Criteria, table: Table<*>): ColumnDeclaring<Boolean> {
        val criterionGroups = criteria.getCriterionGroups()
        val andExpressions = mutableListOf<ColumnDeclaring<Boolean>>()
        criterionGroups.forEach { criterionGroup -> // 第一层元素间是AND关系
            when (criterionGroup) {
                is Array<*> -> { // 第二层元素间是OR关系
                    val orExpressions = mutableListOf<ColumnDeclaring<Boolean>>()
                    criterionGroup.forEach { groupElem ->
                        when (groupElem) {
                            is Criterion -> {
                                convertCriterion(groupElem, table)?.let { orExpressions.add(it) }
                            }
                            is Criteria -> {
                                orExpressions.add(convert(groupElem, table))
                            }
                            else -> {
                                error("Criteria中的元素(数组中)不支持【${criterionGroup::class}】类型！")
                            }
                        }
                    }
                    andExpressions.add(orExpressions.reduce { acc, e -> acc.or(e) })
                }
                is Criterion -> {
                    convertCriterion(criterionGroup, table)?.let { andExpressions.add(it) }
                }
                is Criteria -> {
                    andExpressions.add(convert(criterionGroup, table))
                }
                else -> {
                    error("Criteria中的元素不支持【${criterionGroup::class}】类型！")
                }
            }
        }
        return andExpressions.reduce { acc, e -> acc.and(e) }
    }

    private fun convertCriterion(criterion: Criterion, table: Table<*>): ColumnDeclaring<Boolean>? {
        val column = ColumnHelper.columnOf(table, criterion.property)[criterion.property] as Column<Any>
        val value = criterion.value
        return SqlWhereExpressionFactory.create(column, criterion.operator, value)
    }

}