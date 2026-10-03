package io.kudos.ms.auth.core.organization.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.organization.model.po.AuthManagementRoleGrant
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Ktorm table mapping for [AuthManagementRoleGrant].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthManagementRoleGrants : StringIdTable<AuthManagementRoleGrant>("auth_management_role_grant") {

    var organizationId = varchar("organization_id").bindTo { it.organizationId }

    var userId = varchar("user_id").bindTo { it.userId }

    var roleKind = varchar("role_kind").bindTo { it.roleKind }

    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    var createTime = datetime("create_time").bindTo { it.createTime }
}
