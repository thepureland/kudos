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
        // Guard first, because the failure downstream is a bare UnsupportedOperationException out of
        // `reduce` on an empty list — a stack trace that says nothing about which query built it.
        // An empty Criteria reaching here is nearly always a condition Criteria dropped on the way
        // in: it discards a criterion whose value is null (or an empty string / collection) unless
        // the operator accepts null, so `Criteria.of("status", EQ, null)` silently becomes no
        // condition at all. Refusing it here beats running the caller's query without their filter.
        require(!criteria.isEmpty()) {
            "Criteria for table [${table.tableName}] carries no query condition. Note that Criteria " +
                "drops criterions whose value is null or empty unless the operator accepts null " +
                "(IS_NULL / IS_NOT_NULL / IS_EMPTY / IS_NOT_EMPTY); check the values you passed in."
        }
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
                    val orExpression = orExpressions.reduceOrNull { acc, e -> acc.or(e) }
                        ?: error(
                            "An OR group in the Criteria for table [${table.tableName}] produced no " +
                                "query condition; dropping it would silently widen the query."
                        )
                    andExpressions.add(orExpression)
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
        return andExpressions.reduceOrNull { acc, e -> acc.and(e) }
            ?: error(
                "Criteria for table [${table.tableName}] produced no query condition: [$criteria]. " +
                    "Running the query unfiltered would return rows the caller did not ask for."
            )
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