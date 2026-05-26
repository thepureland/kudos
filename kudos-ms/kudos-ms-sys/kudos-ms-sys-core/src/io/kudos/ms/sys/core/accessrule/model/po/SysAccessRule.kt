package io.kudos.ms.sys.core.accessrule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Access rule DB entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysAccessRule : IManagedDbEntity<String, SysAccessRule> {

    companion object : DbEntityFactory<SysAccessRule>()

    /** Tenant id */
    @get:Sortable
    var tenantId: String

    /** System code */
    @get:Sortable
    var systemCode: String

    /** Access rule type dict code */
    var accessRuleTypeDictCode: String

}
