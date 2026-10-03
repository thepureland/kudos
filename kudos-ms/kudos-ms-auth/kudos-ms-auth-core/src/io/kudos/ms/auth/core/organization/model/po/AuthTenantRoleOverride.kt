package io.kudos.ms.auth.core.organization.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * A member's per-tenant difference from their organization default roles (G-1, G-3).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthTenantRoleOverride : IDbEntity<String, AuthTenantRoleOverride> {

    companion object : DbEntityFactory<AuthTenantRoleOverride>()

    /** Owning organization */
    var organizationId: String

    /** Tenant the difference applies to */
    var tenantId: String

    /** Member account */
    var userId: String

    /** Organization business role */
    var roleId: String

    /** ADD or REMOVE */
    var action: String

    /** Why */
    var reason: String?

    /** Actor */
    var createUserId: String?

    /** When */
    var createTime: LocalDateTime?
}
