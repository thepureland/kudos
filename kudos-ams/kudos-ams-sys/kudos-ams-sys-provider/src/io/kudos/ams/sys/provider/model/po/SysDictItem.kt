package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 字典项数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysDictItem : IMaintainableDbEntity<String, SysDictItem> {
//endregion your codes 1

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


    //region your codes 2

    //endregion your codes 2

}