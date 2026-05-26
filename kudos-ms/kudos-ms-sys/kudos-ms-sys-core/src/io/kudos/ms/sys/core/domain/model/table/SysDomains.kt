package io.kudos.ms.sys.core.domain.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.domain.model.po.SysDomain
import org.ktorm.schema.varchar


/**
 * Domain table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysDomains : ManagedTable<SysDomain>("sys_domain") {

    /** Domain */
    var domain = varchar("domain").bindTo { it.domain }

    /** System code */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}
