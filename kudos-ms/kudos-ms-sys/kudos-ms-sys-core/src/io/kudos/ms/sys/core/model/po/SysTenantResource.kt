package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity

/**
 * 租户-资源关系数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysTenantResource : IDbEntity<String, SysTenantResource> {
//endregion your codes 1

    companion object : DbEntityFactory<SysTenantResource>()

    /** 租户id */
    var tenantId: String

    /** 资源id */
    var resourceId: String


    //region your codes 2

    //endregion your codes 2

}