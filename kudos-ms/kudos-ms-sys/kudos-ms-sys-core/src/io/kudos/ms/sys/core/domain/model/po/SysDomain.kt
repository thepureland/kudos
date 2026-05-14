package io.kudos.ms.sys.core.domain.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 域名数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysDomain : IManagedDbEntity<String, SysDomain> {

    companion object : DbEntityFactory<SysDomain>()

    /** 域名 */
    @get:Sortable
    var domain: String

    /** 系统编码 */
    var systemCode: String

    /** 租户id；`null` 表示平台级（与 DB 中 `tenant_id IS NULL` 对应） */
    var tenantId: String?

}
