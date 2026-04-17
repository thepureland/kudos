package io.kudos.ms.sys.core.accessrule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 访问规则数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysAccessRule : IManagedDbEntity<String, SysAccessRule> {

    companion object : DbEntityFactory<SysAccessRule>()

    /** 租户id */
    @get:Sortable
    var tenantId: String

    /** 系统编码 */
    @get:Sortable
    var systemCode: String

    /** 访问规则类型字典代码 */
    var accessRuleTypeDictCode: String

}
