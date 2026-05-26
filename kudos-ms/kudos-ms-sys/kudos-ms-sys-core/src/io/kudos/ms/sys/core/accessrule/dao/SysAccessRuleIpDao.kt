package io.kudos.ms.sys.core.accessrule.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.data.rdb.ktorm.support.ColumnHelper
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import io.kudos.ms.sys.core.accessrule.model.table.SysAccessRuleIps
import io.kudos.ms.sys.core.accessrule.model.table.SysAccessRules
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import org.ktorm.schema.Column
import org.springframework.stereotype.Repository


/**
 * IP access rule DAO.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysAccessRuleIpDao : BaseCrudDao<String, SysAccessRuleIp, SysAccessRuleIps>() {


    /**
     * Paged join query for IP access rule details and their parent access rules matching the criteria.
     *
     * @param searchPayload query criteria payload
     * @return List<SysAccessRuleIpRow>
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(searchPayload: SysAccessRuleIpQuery): List<SysAccessRuleIpRow> {
        var query = leftJoinSearch(searchPayload)
        val whitelist = sortWhitelistFromPo()
        val allowedOrders = filterOrdersBySortWhitelist(searchPayload.orders, whitelist)
        if (allowedOrders.isEmpty()) {
            val orderList = listOf(
                SysAccessRules.systemCode.asc(),
                SysAccessRules.tenantId.asc(),
                SysAccessRuleIps.ipStart.asc()
            )
            query = query.orderBy(*orderList.toTypedArray())
        } else {
            val orderExps = mutableListOf<OrderByExpression>()
            allowedOrders.forEach {
                //TODO
                var columns = try {
                    ColumnHelper.columnOf(SysAccessRules, it.property)
                } catch (_: IllegalStateException) {
                    emptyMap()
                }
                if (columns.isEmpty()) {
                    columns = ColumnHelper.columnOf(SysAccessRuleIps, it.property)
                }
                if (columns.isEmpty()) {
                    throw ObjectNotFoundException("No column found for property [${it.property}]!")
                }
                val column = requireNotNull(columns[it.property]) {
                    "No column found for property [${it.property}]!"
                }
                if (it.isAscending()) {
                    orderExps.add(column.asc())
                } else {
                    orderExps.add(column.desc())
                }
            }
            query = query.orderBy(*orderExps.toTypedArray())
        }
        val rawPageNo = searchPayload.pageNo
        val pageNo = when {
            rawPageNo != null -> maxOf(1, rawPageNo)
            searchPayload.isUnpagedSearchAllowed() -> null
            else -> 1
        }
        if (pageNo != null) {
            val rawSize = searchPayload.pageSize ?: 10
            val pageSize = minOf(rawSize, searchPayload.getMaxPageSize())
            query = query.limit((pageNo - 1) * pageSize, pageSize)
        }

        @Suppress("UNCHECKED_CAST")
        val extraColumns = mapOf(
            SysAccessRuleIpRow::parentRuleActive.name to (SysAccessRules.active as Column<Any>),
            SysAccessRuleIpRow::tenantId.name to (SysAccessRules.tenantId as Column<Any>),
            SysAccessRuleIpRow::systemCode.name to (SysAccessRules.systemCode as Column<Any>),
            SysAccessRuleIpRow::accessRuleTypeDictCode.name to (SysAccessRules.accessRuleTypeDictCode as Column<Any>)
        )

        return query.map { row ->
            mapTo(row, SysAccessRuleIpRow::class, extraColumns = extraColumns)
        }
    }

    /**
     * Count IP access rule details (joined with their parent access rules) matching the criteria.
     *
     * @param searchPayload query criteria
     * @return number of matching rows
     * @author K
     * @since 1.0.0
     */
    fun count(searchPayload: SysAccessRuleIpQuery): Int {
        return leftJoinSearch(searchPayload).totalRecordsInAllPages
    }

    /**
     * Build the query: SysAccessRuleIps LEFT JOIN SysAccessRules with WHERE clauses applied.
     *
     * @param searchPayload query criteria payload
     * @return Query
     * @author K
     * @since 1.0.0
     */
    fun leftJoinSearch(searchPayload: SysAccessRuleIpQuery): Query {
        var onExpr = SysAccessRuleIps.parentRuleId.eq(SysAccessRules.id)
        searchPayload.active?.let { onExpr = onExpr and SysAccessRuleIps.active.eq(it) }

        val querySource = database()
            .from(SysAccessRuleIps)
            .leftJoin(SysAccessRules, on = onExpr)

        return querySource.select().whereWithConditions {
            val id = searchPayload.id
            if (!id.isNullOrBlank()) {
                it += SysAccessRuleIps.id.eq(id)
            }
            val parentRuleId = searchPayload.parentRuleId
            if (!parentRuleId.isNullOrBlank()) {
                it += SysAccessRuleIps.parentRuleId.eq(parentRuleId)
            }
            searchPayload.parentRuleActive?.let { active -> it += SysAccessRules.active.eq(active) }
            val tenantId = searchPayload.tenantId
            if (tenantId == null && searchPayload.getNullProperties()?.contains(SysAccessRule::tenantId.name) == true) {
                it += SysAccessRules.tenantId.isNull()
            } else if (!tenantId.isNullOrBlank()) {
                whereExpr(SysAccessRules.tenantId, OperatorEnum.EQ, tenantId.trim())?.let { expr ->
                    it += expr
                }
            }
            val systemCode = searchPayload.systemCode
            if (!systemCode.isNullOrBlank()) {
                whereExpr(
                    SysAccessRules.systemCode,
                    OperatorEnum.EQ,
                    systemCode.trim()
                )?.let { expr -> it += expr }
            }
            val accessRuleTypeDictCode = searchPayload.accessRuleTypeDictCode
            if (!accessRuleTypeDictCode.isNullOrBlank()) {
                whereExpr(
                    SysAccessRules.accessRuleTypeDictCode,
                    OperatorEnum.EQ,
                    accessRuleTypeDictCode.trim()
                )?.let { expr -> it += expr }
            }
            val ipTypeDictCode = searchPayload.ipTypeDictCode
            if (!ipTypeDictCode.isNullOrBlank()) {
                whereExpr(
                    SysAccessRuleIps.ipTypeDictCode,
                    OperatorEnum.EQ,
                    ipTypeDictCode.trim()
                )?.let { expr -> it += expr }
            }
        }
    }

    /**
     * Delete IP rules by parent rule ID.
     *
     * @param ruleId parent rule ID
     * @return number of rows deleted
     */
    fun deleteByParentRuleId(ruleId: String): Int {
        val criteria = Criteria(SysAccessRuleIp::parentRuleId eq ruleId)
        return batchDeleteCriteria(criteria)
    }

//    private fun mapRowToRecord(row: QueryRowSet) = SysAccessRuleIpRow(
//        id = row[SysAccessRuleIps.id] ?: "",
//        ipStart = row[SysAccessRuleIps.ipStart],
//        ipEnd = row[SysAccessRuleIps.ipEnd],
//        ipTypeDictCode = row[SysAccessRuleIps.ipTypeDictCode],
//        expirationTime = row[SysAccessRuleIps.expirationTime],
//        parentRuleId = row[SysAccessRuleIps.parentRuleId] ?: row[SysAccessRules.id],
//        remark = row[SysAccessRuleIps.remark],
//        active = row[SysAccessRuleIps.active],
//        parentRuleActive = row[SysAccessRules.active],
//        tenantId = row[SysAccessRules.tenantId],
//        systemCode = row[SysAccessRules.systemCode],
//        accessRuleTypeDictCode = row[SysAccessRules.accessRuleTypeDictCode]
//    )


}
