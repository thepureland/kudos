package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 访问规则数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysAccessRule : IMaintainableDbEntity<String, SysAccessRule> {
//endregion your codes 1

    companion object : DbEntityFactory<SysAccessRule>()

    /** 租户id */
    var tenantId: String

    /** 系统编码 */
    var systemCode: String

    /** 规则类型字典代码 */
    var ruleTypeDictCode: String


    //region your codes 2

    //endregion your codes 2

}
