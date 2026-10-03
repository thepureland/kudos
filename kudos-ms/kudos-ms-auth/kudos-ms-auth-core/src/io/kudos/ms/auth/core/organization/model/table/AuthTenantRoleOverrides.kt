package io.kudos.ms.auth.core.organization.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.organization.model.po.AuthTenantRoleOverride
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Ktorm table mapping for [AuthTenantRoleOverride].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthTenantRoleOverrides : StringIdTable<AuthTenantRoleOverride>("auth_tenant_role_override") {

    var organizationId = varchar("organization_id").bindTo { it.organizationId }

    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    var userId = varchar("user_id").bindTo { it.userId }

    var roleId = varchar("role_id").bindTo { it.roleId }

    var action = varchar("action").bindTo { it.action }

    var reason = varchar("reason").bindTo { it.reason }

    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    var createTime = datetime("create_time").bindTo { it.createTime }
}
