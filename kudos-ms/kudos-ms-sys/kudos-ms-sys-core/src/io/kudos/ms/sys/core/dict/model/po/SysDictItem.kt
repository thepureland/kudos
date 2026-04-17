package io.kudos.ms.sys.core.dict.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 字典项数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysDictItem : IManagedDbEntity<String, SysDictItem> {

    companion object : DbEntityFactory<SysDictItem>()

    /** 字典项代码 */
    @get:Sortable
    var itemCode: String

    /** 字典项名称 */
    @get:Sortable
    var itemName: String

    /** 字典id */
    var dictId: String

    /** 字典项排序 */
    var orderNum: Int?

    /** 父id */
    var parentId: String?

}