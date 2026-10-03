package io.kudos.ms.auth.core.organization.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Built-in management role grant (G-6, G-7, G-11).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthManagementRoleGrant : IDbEntity<String, AuthManagementRoleGrant> {

    companion object : DbEntityFactory<AuthManagementRoleGrant>()

    /** Organization */
    var organizationId: String

    /** Holder */
    var userId: String

    /** ORGANIZATION_PERMISSION_ADMIN or TENANT_PERMISSION_ADMIN */
    var roleKind: String

    /** Governed tenant; empty for ORGANIZATION_PERMISSION_ADMIN */
    var tenantId: String

    /** Granted by */
    var createUserId: String?

    /** When */
    var createTime: LocalDateTime?
}
