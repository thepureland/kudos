package io.kudos.ms.sys.core.tenant.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.tenant.model.po.SysTenantResource
import org.ktorm.schema.varchar


/**
 * 租户-资源关系数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysTenantResources : StringIdTable<SysTenantResource>("sys_tenant_resource") {

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 资源id */
    var resourceId = varchar("resource_id").bindTo { it.resourceId }




}