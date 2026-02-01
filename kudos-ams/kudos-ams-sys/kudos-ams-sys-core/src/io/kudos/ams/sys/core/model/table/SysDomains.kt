package io.kudos.ams.sys.core.model.table

import io.kudos.ams.sys.core.model.po.SysDomain
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 域名数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysDomains : MaintainableTable<SysDomain>("sys_domain") {
//endregion your codes 1

    /** 域名 */
    var domain = varchar("domain").bindTo { it.domain }

    /** 子系统编码 */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }

    /** 系统编码 */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }


    //region your codes 2

    //endregion your codes 2

}