package io.kudos.ams.sys.service.dao

import io.kudos.ams.sys.service.model.po.SysDictItem
import io.kudos.ams.sys.service.model.table.SysDictItems
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList


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

    fun searchByDictId(dictId: String): List<SysDictItem> {
        return entitySequence().filter { SysDictItems.dictId eq dictId }.sortedBy { SysDictItems.orderNum }.toList()
//        return querySource()
//            .select(RegDictItems.columns)
//            .orderBy()
//            .where {  }
//            .map { row -> RegDictItems.createEntity(row) }
//            .toList()
    }

    //endregion your codes 2

}