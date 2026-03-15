package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.model.po.SysDomain
import org.ktorm.schema.varchar


/**
 * 域名数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysDomains : ManagedTable<SysDomain>("sys_domain") {

    /** 域名 */
    var domain = varchar("domain").bindTo { it.domain }

    /** 系统编码 */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}
