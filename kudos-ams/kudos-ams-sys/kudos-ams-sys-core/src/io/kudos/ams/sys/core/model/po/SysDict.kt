package io.kudos.ams.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 字典数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysDict : IMaintainableDbEntity<String, SysDict> {
//endregion your codes 1

    companion object : DbEntityFactory<SysDict>()

    /** 字典类型 */
    var dictType: String

    /** 字典名称 */
    var dictName: String

    /** 原子服务编码 */
    var atomicServiceCode: String


    //region your codes 2

    //endregion your codes 2

}