package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.data.rdb.ktorm.support.ColumnHelper
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRecord
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.ms.sys.core.model.table.SysDictItems
import io.kudos.ms.sys.core.model.table.SysDicts
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.ktorm.expression.OrderByExpression
import org.springframework.stereotype.Repository


/**
 * 字典项数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDictItemDao : BaseCrudDao<String, SysDictItem, SysDictItems>() {
//endregion your codes 1

    //region your codes 2

    fun searchActiveItemByDictId(dictId: String): List<SysDictItem> {
        return entitySequence().filter {
            (SysDictItems.dictId eq dictId).and(SysDictItems.active eq true)
        }.sortedBy { SysDictItems.orderNum }.toList()
//        return querySource()
//            .select(SysDictItems.columns)
//            .orderBy()
//            .where {  }
//            .map { row -> SysDictItems.createEntity(row) }
//            .toList()
    }

    /**
     * 分页连接查询符合条件的字典项及字典
     *
     * @param searchPayload 查询项载体
     * @return List<SysDictItemRecord>
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(searchPayload: SysDictItemSearchPayload): List<SysDictItemRecord> {
        var query = leftJoinSearch(searchPayload)
        val orders = searchPayload.orders
        if (orders.isNullOrEmpty()) {
            val orderList = mutableListOf(SysDicts.atomicServiceCode.asc(), SysDicts.dictType.asc())
            orderList.add(SysDictItems.orderNum.asc())
            query = query.orderBy(*orderList.toTypedArray())
        } else {
            val orderExps = mutableListOf<OrderByExpression>()
            orders.forEach {
                //TODO
                var columns = try {
                    ColumnHelper.columnOf(SysDicts, it.property)
                } catch (_: IllegalStateException) {
                    emptyMap()
                }
                if (columns.isEmpty()) {
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
            SysDictItemRecord().apply {
                atomicServiceCode = row[SysDicts.atomicServiceCode]
                dictId = row[SysDicts.id]
                dictType = row[SysDicts.dictType]
                dictName = row[SysDicts.dictName]
                itemId = row[SysDictItems.id]
                itemCode = row[SysDictItems.itemCode]
                parentId = row[SysDictItems.parentId]
                itemName = row[SysDictItems.itemName]
                orderNum = row[SysDictItems.orderNum]
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
    fun count(searchPayload: SysDictItemSearchPayload): Int {
        return leftJoinSearch(searchPayload).totalRecordsInAllPages
    }

    /**
     * 构造SysDictItems左连接SysDicts的带有where查询条件的查询对象
     *
     * @param searchPayload 查询项载体
     * @return Query
     * @author K
     * @since 1.0.0
     */
    fun leftJoinSearch(searchPayload: SysDictItemSearchPayload): Query {
        val querySource = database()
            .from(SysDictItems)
            .leftJoin(SysDicts, on = SysDictItems.dictId.eq(SysDicts.id))

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
                if (searchPayload.dictActive != null) {
                    it += SysDicts.active.eq(searchPayload.dictActive!!)
                }
                if (!searchPayload.atomicServiceCode.isNullOrBlank()) {
                    it += whereExpr(SysDicts.atomicServiceCode, OperatorEnum.ILIKE, searchPayload.atomicServiceCode!!.trim())!!
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
                if (!searchPayload.itemName.isNullOrBlank()) {
                    it += whereExpr(SysDictItems.itemName, OperatorEnum.ILIKE, searchPayload.itemName!!.trim())!!
                }
            }
    }

    //endregion your codes 2

}