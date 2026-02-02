package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 域名数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysDomain : IMaintainableDbEntity<String, SysDomain> {
//endregion your codes 1

    companion object : DbEntityFactory<SysDomain>()

    /** 域名 */
    var domain: String

    /** 系统编码 */
    var systemCode: String

    /** 租户id */
    var tenantId: String


    //region your codes 2

    //endregion your codes 2

}
