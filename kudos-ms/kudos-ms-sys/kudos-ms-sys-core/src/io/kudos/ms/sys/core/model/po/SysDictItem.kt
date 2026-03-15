package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

/**
 * 字典项数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysDictItem : IManagedDbEntity<String, SysDictItem> {

    companion object : DbEntityFactory<SysDictItem>()

    /** 字典项代码 */
    var itemCode: String

    /** 字典项名称 */
    var itemName: String

    /** 字典id */
    var dictId: String

    /** 字典项排序 */
    var orderNum: Int?

    /** 父id */
    var parentId: String?

}