package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 参数数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysParam : IMaintainableDbEntity<String, SysParam> {
//endregion your codes 1

    companion object : DbEntityFactory<SysParam>()

    /** 参数名称 */
    var paramName: String

    /** 参数值 */
    var paramValue: String

    /** 默认参数值 */
    var defaultValue: String?

    /** 模块 */
    var moduleCode: String

    /** 序号 */
    var orderNum: Int?


    //region your codes 2

    //endregion your codes 2

}