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
open class SysDictItemDao : BaseCrudDao<String, SysDictItem, SysDictItems>() {


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


}