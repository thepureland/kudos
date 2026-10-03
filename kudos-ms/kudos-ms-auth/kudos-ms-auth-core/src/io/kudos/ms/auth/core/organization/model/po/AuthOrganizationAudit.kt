package io.kudos.ms.auth.core.organization.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Audit record of an organization authorization change.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthOrganizationAudit : IDbEntity<String, AuthOrganizationAudit> {

    companion object : DbEntityFactory<AuthOrganizationAudit>()

    /** Organization */
    var organizationId: String

    /** Actor; null for trusted internal work */
    var actorId: String?

    /** What happened */
    var action: String

    /** Tenant concerned */
    var tenantId: String?

    /** Member concerned */
    var targetUserId: String?

    /** Role, grant or other object concerned */
    var targetId: String?

    /** Before/after summary */
    var detail: String?

    /** Why */
    var reason: String?

    /** When */
    var createTime: LocalDateTime
}
