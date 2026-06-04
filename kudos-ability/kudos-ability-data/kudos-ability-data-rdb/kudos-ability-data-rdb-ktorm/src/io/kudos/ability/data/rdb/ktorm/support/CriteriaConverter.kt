package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import org.ktorm.dsl.and
import org.ktorm.dsl.or
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * Criteria converter: turns a Criteria into a Ktorm query-condition expression.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal object CriteriaConverter {

    /**
     * Converts a Criteria into a Ktorm expression.
     *
     * @param criteria Criteria
     * @param table Ktorm table object
     * @return Ktorm query-condition expression
     * @author K
     * @since 1.0.0
     */
    fun convert(criteria: Criteria, table: Table<*>): ColumnDeclaring<Boolean> {
        val criterionGroups = criteria.getCriterionGroups()
        val andExpressions = mutableListOf<ColumnDeclaring<Boolean>>()
        criterionGroups.forEach { criterionGroup -> // Top-level elements combine with AND
            when (criterionGroup) {
                is Array<*> -> { // Second-level elements combine with OR
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
                                error("Unsupported element type [${criterionGroup::class}] inside Criteria array!")
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
                    error("Unsupported element type [${criterionGroup::class}] in Criteria!")
                }
            }
        }
        return andExpressions.reduce { acc, e -> acc.and(e) }
    }

    /**
     * 单条 [Criterion] → ktorm `ColumnDeclaring<Boolean>` 的转换：
     * 走 [ColumnHelper] 查列、再交给 [SqlWhereExpressionFactory] 按操作符生成具体 SQL 表达式。
     *
     * @param criterion 业务条件
     * @param table ktorm 表对象
     * @return 列条件表达式；不支持的操作符返回 null
     * @author K
     * @since 1.0.0
     */
    private fun convertCriterion(criterion: Criterion, table: Table<*>): ColumnDeclaring<Boolean>? {
        val column = ColumnHelper.columnOf(table, criterion.property)[criterion.property] as Column<Any>
        val value = criterion.value
        return SqlWhereExpressionFactory.create(column, criterion.operator, value)
    }

}