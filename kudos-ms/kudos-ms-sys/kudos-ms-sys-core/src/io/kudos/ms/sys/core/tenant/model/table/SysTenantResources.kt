package io.kudos.ms.sys.core.tenant.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.tenant.model.po.SysTenantResource
import org.ktorm.schema.varchar


/**
 * Tenant-resource relation table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysTenantResources : StringIdTable<SysTenantResource>("sys_tenant_resource") {

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Resource id */
    var resourceId = varchar("resource_id").bindTo { it.resourceId }




}