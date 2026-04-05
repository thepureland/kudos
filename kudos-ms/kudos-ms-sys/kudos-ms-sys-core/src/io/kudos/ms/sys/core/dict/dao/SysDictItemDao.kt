package io.kudos.ms.sys.core.dict.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.dict.model.po.SysDictItem
import io.kudos.ms.sys.core.dict.model.table.SysDictItems
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
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