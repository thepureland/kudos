package io.kudos.ams.sys.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.data.rdb.ktorm.support.ColumnHelper
import io.kudos.ams.sys.common.vo.accessruleip.SysAccessRuleIpRecord
import io.kudos.ams.sys.common.vo.accessruleip.SysAccessRuleIpSearchPayload
import io.kudos.ams.sys.provider.model.po.SysAccessRule
import io.kudos.ams.sys.provider.model.po.SysAccessRuleIp
import io.kudos.ams.sys.provider.model.table.SysAccessRuleIps
import io.kudos.ams.sys.provider.model.table.SysAccessRules
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.enums.OperatorEnum
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import org.springframework.stereotype.Repository


/**
 * ip访问规则数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysAccessRuleIpDao : BaseCrudDao<String, SysAccessRuleIp, SysAccessRuleIps>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 分页连接查询符合条件的ip访问规则明细和父访问规则
     *
     * @param searchPayload 查询项载体
     * @return List<SysAccessRuleIpRecord>
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(searchPayload: SysAccessRuleIpSearchPayload): List<SysAccessRuleIpRecord> {
        var query = leftJoinSearch(searchPayload)
        val orders = searchPayload.orders
        if (orders.isNullOrEmpty()) {
            val orderList = listOf(
                SysAccessRules.subSystemCode.asc(),
                SysAccessRules.tenantId.asc(),
                SysAccessRuleIps.ipStart.asc()
            )
            query = query.orderBy(*orderList.toTypedArray())
        } else {
            val orderExps = mutableListOf<OrderByExpression>()
            orders.forEach {
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
                    throw ObjectNotFoundException("根据属性【${it.property}】找不到对应的列!")
                }
                val column = columns[it.property]!!
                if (it.isAscending()) {
                    orderExps.add(column.asc())
                } else {
                    orderExps.add(column.desc())
                }
            }
            query = query.orderBy(*orderExps.toTypedArray())
        }
        val pageNo = searchPayload.pageNo
        if (pageNo != null) {
            val pageSize = searchPayload.pageSize ?: 10
            query = query.limit((pageNo - 1) * pageSize, pageSize)
        }

        return query.map { row ->
            SysAccessRuleIpRecord().apply {
                id = row[SysAccessRuleIps.id]
                ipStart = row[SysAccessRuleIps.ipStart]
                ipEnd = row[SysAccessRuleIps.ipEnd]
                ipTypeDictCode = row[SysAccessRuleIps.ipTypeDictCode]
                expirationTime = row[SysAccessRuleIps.expirationTime]
                parentRuleId = row[SysAccessRuleIps.parentRuleId] ?: row[SysAccessRules.id]
                remark = row[SysAccessRuleIps.remark]
                active = row[SysAccessRuleIps.active]
                remark = row[SysAccessRuleIps.remark]
                parentRuleActive = row[SysAccessRules.active]
                tenantId = row[SysAccessRules.tenantId]
                subSystemCode = row[SysAccessRules.subSystemCode]
                ruleTypeDictCode = row[SysAccessRules.tenantId]
            }
        }
    }

    /**
     * 连接查询符合条件的ip访问规则明细和父访问规则的数量
     *
     * @param searchPayload 查询参数
     * @return 结果数
     * @author K
     * @since 1.0.0
     */
    fun count(searchPayload: SysAccessRuleIpSearchPayload): Int {
        return leftJoinSearch(searchPayload).totalRecordsInAllPages
    }

    /**
     * 构造SysAccessRuleIps左连接SysAccessRules的带有where查询条件的查询对象
     *
     * @param searchPayload 查询项载体
     * @return Query
     * @author K
     * @since 1.0.0
     */
    fun leftJoinSearch(searchPayload: SysAccessRuleIpSearchPayload): Query {
        var onExpr = SysAccessRuleIps.parentRuleId.eq(SysAccessRules.id)
        if (searchPayload.active != null) {
            onExpr =onExpr and  SysAccessRuleIps.active.eq(searchPayload.active!!)
        }

        val querySource = database()
            .from(SysAccessRules)
            .leftJoin(SysAccessRuleIps, on = onExpr)

        return querySource.select().whereWithConditions {
            if (!searchPayload.id.isNullOrBlank()) {
                it += SysAccessRuleIps.id.eq(searchPayload.id!!)
            }
            if (!searchPayload.parentRuleId.isNullOrBlank()) {
                it += SysAccessRuleIps.parentRuleId.eq(searchPayload.parentRuleId!!)
            }
            if (searchPayload.parentRuleActive != null) {
                it += SysAccessRules.active.eq(searchPayload.parentRuleActive!!)
            }
            if (searchPayload.tenantId == null && searchPayload.nullProperties?.contains(SysAccessRule::tenantId.name) == true) {
                it += SysAccessRules.tenantId.isNull()
            } else if (!searchPayload.tenantId.isNullOrBlank()) {
                it += whereExpr(SysAccessRules.tenantId, OperatorEnum.EQ, searchPayload.tenantId!!.trim())!!
            }
            if (!searchPayload.subSystemCode.isNullOrBlank()) {
                it += whereExpr(
                    SysAccessRules.subSystemCode,
                    OperatorEnum.EQ,
                    searchPayload.subSystemCode!!.trim()
                )!!
            }
            if (!searchPayload.ruleTypeDictCode.isNullOrBlank()) {
                it += whereExpr(
                    SysAccessRules.ruleTypeDictCode,
                    OperatorEnum.EQ,
                    searchPayload.ruleTypeDictCode!!.trim()
                )!!
            }
            if (!searchPayload.ipTypeDictCode.isNullOrBlank()) {
                it += whereExpr(
                    SysAccessRuleIps.ipTypeDictCode,
                    OperatorEnum.EQ,
                    searchPayload.ipTypeDictCode!!.trim()
                )!!
            }
        }
    }

    //endregion your codes 2

}