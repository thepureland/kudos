package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.data.rdb.ktorm.support.ColumnHelper
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.ms.sys.core.model.table.SysDictItems
import io.kudos.ms.sys.core.model.table.SysDicts
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.ktorm.expression.OrderByExpression
import org.ktorm.schema.Column
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
     * @return List<SysDictItemRow>
     * @author K
     * @since 1.0.0
     */
    fun pagingSearch(searchPayload: SysDictItemQuery): List<SysDictItemRow> {
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
                val column = requireNotNull(columns[it.property]) {
                    "根据属性【${it.property}】找不到对应的列!"
                }
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

        @Suppress("UNCHECKED_CAST")
        val recordExtraColumns = mapOf(
            "atomicServiceCode" to (SysDicts.atomicServiceCode as Column<Any>),
            "dictType" to (SysDicts.dictType as Column<Any>),
            "dictName" to (SysDicts.dictName as Column<Any>),
            "itemId" to (SysDictItems.id as Column<Any>)
        )

        return query.map { row ->
            mapTo(
                row,
                SysDictItemRow::class,
                extraColumns = recordExtraColumns
            )
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
    fun count(searchPayload: SysDictItemQuery): Int {
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
    fun leftJoinSearch(searchPayload: SysDictItemQuery): Query {
        val querySource = database()
            .from(SysDictItems)
            .leftJoin(SysDicts, on = SysDictItems.dictId.eq(SysDicts.id))

        return querySource.select()
            .whereWithConditions {
                val id = searchPayload.id
                if (!id.isNullOrBlank()) {
                    it += SysDictItems.id.eq(id)
                }
                val parentId = searchPayload.parentId
                if (!parentId.isNullOrBlank()) {
                    it += SysDictItems.parentId.eq(parentId)
                }
                val active = searchPayload.active
                if (active != null) {
                    it += SysDictItems.active.eq(active)
                }
                val dictActive = searchPayload.dictActive
                if (dictActive != null) {
                    it += SysDicts.active.eq(dictActive)
                }
                val atomicServiceCode = searchPayload.atomicServiceCode
                if (!atomicServiceCode.isNullOrBlank()) {
                    whereExpr(SysDicts.atomicServiceCode, OperatorEnum.ILIKE, atomicServiceCode.trim())?.let { expr ->
                        it += expr
                    }
                }
                val dictType = searchPayload.dictType
                if (!dictType.isNullOrBlank()) {
                    whereExpr(SysDicts.dictType, OperatorEnum.ILIKE, dictType.trim())?.let { expr ->
                        it += expr
                    }
                }
                val dictName = searchPayload.dictName
                if (!dictName.isNullOrBlank()) {
                    whereExpr(SysDicts.dictName, OperatorEnum.ILIKE, dictName.trim())?.let { expr ->
                        it += expr
                    }
                }
                val itemCode = searchPayload.itemCode
                if (!itemCode.isNullOrBlank()) {
                    whereExpr(SysDictItems.itemCode, OperatorEnum.ILIKE, itemCode.trim())?.let { expr ->
                        it += expr
                    }
                }
                val itemName = searchPayload.itemName
                if (!itemName.isNullOrBlank()) {
                    whereExpr(SysDictItems.itemName, OperatorEnum.ILIKE, itemName.trim())?.let { expr ->
                        it += expr
                    }
                }
            }
    }

    /**
     * 根据原子服务编码查询字典节点（用于树加载）
     */
    fun searchDictNodesByAtomicServiceCode(atomicServiceCode: String): List<SysDictTreeNode> {
        val query = database().from(SysDicts)
            .select(SysDicts.id, SysDicts.dictType)
            .where { SysDicts.atomicServiceCode eq atomicServiceCode }
            .orderBy(SysDicts.dictType.asc())
        return query.map { row ->
            SysDictTreeNode().apply {
                id = row[SysDicts.id] ?: ""
                code = row[SysDicts.dictType]
            }
        }
    }

    /**
     * 查询指定父节点的直接孩子节点（用于树加载）
     */
    fun searchDirectChildrenNodes(parentId: String, activeOnly: Boolean): List<SysDictTreeNode> {
        val searchPayload = SysDictItemQuery(
            parentId = parentId,
            active = if (activeOnly) true else null
        )
        return leftJoinSearch(searchPayload)
            .orderBy(SysDictItems.orderNum.asc())
            .map { row ->
                SysDictTreeNode().apply {
                    id = row[SysDictItems.id] ?: ""
                    code = row[SysDictItems.itemCode]
                }
            }
    }

    //endregion your codes 2

}