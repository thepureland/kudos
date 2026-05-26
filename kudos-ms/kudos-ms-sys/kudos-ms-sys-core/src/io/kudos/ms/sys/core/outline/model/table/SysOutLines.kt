package io.kudos.ms.sys.core.outline.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Outbound allowlist table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysOutLines : ManagedTable<SysOutLine>("sys_out_line") {

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Host or wildcard */
    var host = varchar("host").bindTo { it.host }

    /** Port; NULL means any port */
    var port = int("port").bindTo { it.port }

    /** Protocol */
    var protocol = varchar("protocol").bindTo { it.protocol }

    /** System code */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

}
