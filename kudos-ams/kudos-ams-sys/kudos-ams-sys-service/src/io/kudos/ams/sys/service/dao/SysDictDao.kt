package io.kudos.ams.sys.service.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.data.rdb.ktorm.support.ColumnHelper
import io.kudos.ams.sys.common.vo.dict.SysDictRecord
import io.kudos.ams.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ams.sys.service.model.po.SysDict
import io.kudos.ams.sys.service.model.table.SysDictItems
import io.kudos.ams.sys.service.model.table.SysDicts
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.enums.OperatorEnum
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import org.springframework.stereotype.Repository


/**
 * 字典数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDictDao : BaseCrudDao<String, SysDict, SysDicts>() {
//endregion your codes 1

    //region your codes 2

    @Suppress("UNCHECKED_CAST")
    fun getDictIdByModuleAndType(moduleCode: String, type: String): String? {
        val list = querySource()
            .select(SysDicts.id)
            .whereWithConditions {
                it += SysDicts.moduleCode eq moduleCode
                it += SysDicts.dictType eq type
            }
            .map { row -> row[SysDicts.id] }
            .toList() as List<String>
        return if (list.isEmpty()) null else list.first()
    }

    /**
     * 分页连接查询符合条件的字典项及字典
     *
     * @param searchPayload 查询项载体
     * @return List(RegDictListRecord)
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(searchPayload: SysDictSearchPayload): List<SysDictRecord> {
        var query = leftJoinSearch(searchPayload)
        val orders = searchPayload.orders
        if (orders.isNullOrEmpty()) {
            val orderList = mutableListOf(SysDicts.moduleCode.asc(), SysDicts.dictType.asc())
            if (!searchPayload.isDict) {
                orderList.add(SysDictItems.orderNum.asc())
            }
            query = query.orderBy(*orderList.toTypedArray())
        } else {
            val orderExps = mutableListOf<OrderByExpression>()
            orders.forEach {
                var columns = try {
                    ColumnHelper.columnOf(SysDicts, it.property)
                } catch (e: IllegalStateException) {
                    emptyMap()
                }
                if (columns.isEmpty() && !searchPayload.isDict) {
                    columns = ColumnHelper.columnOf(SysDictItems, it.property)
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
            SysDictRecord().apply {
                moduleCode = row[SysDicts.moduleCode]
                dictId = row[SysDicts.id]
                dictType = row[SysDicts.dictType]
                dictName = row[SysDicts.dictName]
                itemId = row[SysDictItems.id]
                itemCode = row[SysDictItems.itemCode]
                parentId = row[SysDictItems.parentId]
                itemName = row[SysDictItems.itemName]
                seqNo = row[SysDictItems.orderNum]
                active = row[SysDictItems.active]
                remark = row[SysDictItems.remark]
            }
        }
    }

    /**
     * 连接查询符合条件的字典项及字典的数量
     *
     * @param searchPayload 查询参数
     * @return 结果数
     * @author K
     * @since 1.0.0
     */
    fun count(searchPayload: SysDictSearchPayload): Int {
        return leftJoinSearch(searchPayload).totalRecordsInAllPages
    }

    /**
     * 构造RegDictItems左连接RegDicts的带有where查询条件的查询对象
     *
     * @param searchPayload 查询项载体
     * @return Query
     * @author K
     * @since 1.0.0
     */
    fun leftJoinSearch(searchPayload: SysDictSearchPayload): Query {
        val querySource = if (searchPayload.isDict) {
            searchPayload.active = null // reg_dict表无此字段
            database().from(SysDicts)
        } else {
            database().from(SysDictItems).leftJoin(
                SysDicts, on = SysDictItems.dictId.eq(
                    SysDicts.id
                )
            )
        }

        return querySource.select()
            .whereWithConditions {
                if (!searchPayload.id.isNullOrBlank()) {
                    it += SysDictItems.id.eq(searchPayload.id!!)
                }
                if (!searchPayload.parentId.isNullOrBlank()) {
                    it += SysDictItems.parentId.eq(searchPayload.parentId!!)
                }
                if (searchPayload.active != null) {
                    it += SysDictItems.active.eq(searchPayload.active!!)
                }
                if (!searchPayload.module.isNullOrBlank()) {
                    it += whereExpr(SysDicts.moduleCode, OperatorEnum.ILIKE, searchPayload.module!!.trim())!!
                }
                if (!searchPayload.dictType.isNullOrBlank()) {
                    it += whereExpr(SysDicts.dictType, OperatorEnum.ILIKE, searchPayload.dictType!!.trim())!!
                }
                if (!searchPayload.dictName.isNullOrBlank()) {
                    it += whereExpr(SysDicts.dictName, OperatorEnum.ILIKE, searchPayload.dictName!!.trim())!!
                }
                if (!searchPayload.itemCode.isNullOrBlank()) {
                    it += whereExpr(SysDictItems.itemCode, OperatorEnum.ILIKE, searchPayload.itemCode!!.trim())!!
                }
//                if (StringKit.isNotBlank(searchPayload.parentCode)) {
//                    it += whereExpr(RegDictItems.parentCode, Operator.ILIKE, searchPayload.parentCode!!.trim())!!
//                }
                if (!searchPayload.itemName.isNullOrBlank()) {
                    it += whereExpr(SysDictItems.itemName, OperatorEnum.ILIKE, searchPayload.itemName!!.trim())!!
                }
            }
    }

    //endregion your codes 2

}