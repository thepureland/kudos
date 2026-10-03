package io.kudos.ms.auth.core.organization.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.organization.model.po.AuthTenantRoleOverride
import io.kudos.ms.auth.core.organization.model.table.AuthTenantRoleOverrides
import org.springframework.stereotype.Repository

/**
 * DAO for per-tenant role overrides.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthTenantRoleOverrideDao : BaseCrudDao<String, AuthTenantRoleOverride, AuthTenantRoleOverrides>() {

    /** All overrides of [userId] across the tenants of [organizationId]. */
    open fun searchByUser(organizationId: String, userId: String): List<AuthTenantRoleOverride> =
        search(Criteria.and(AuthTenantRoleOverride::organizationId eq organizationId, AuthTenantRoleOverride::userId eq userId))

    /** Overrides of [userId] in [tenantId]. */
    open fun searchByTenantAndUser(tenantId: String, userId: String): List<AuthTenantRoleOverride> =
        search(Criteria.and(AuthTenantRoleOverride::tenantId eq tenantId, AuthTenantRoleOverride::userId eq userId))

    /** All overrides in [tenantId]. */
    open fun searchByTenant(tenantId: String): List<AuthTenantRoleOverride> =
        search(Criteria(AuthTenantRoleOverride::tenantId eq tenantId))

    /** The override of ([tenantId], [userId], [roleId]), if any. */
    open fun find(tenantId: String, userId: String, roleId: String): AuthTenantRoleOverride? =
        search(Criteria.and(
            AuthTenantRoleOverride::tenantId eq tenantId,
            AuthTenantRoleOverride::userId eq userId,
            AuthTenantRoleOverride::roleId eq roleId,
        )).firstOrNull()

    /** Deletes every override of [tenantId]; used when the tenant leaves its organization. */
    open fun deleteByTenant(tenantId: String): Int =
        batchDeleteCriteria(Criteria(AuthTenantRoleOverride::tenantId eq tenantId))

    /** Deletes every override naming [roleId]; used when the role is deleted. */
    open fun deleteByRole(roleId: String): Int =
        batchDeleteCriteria(Criteria(AuthTenantRoleOverride::roleId eq roleId))

    /** Deletes every override of [userId]; used when the membership ends. */
    open fun deleteByUser(userId: String): Int =
        batchDeleteCriteria(Criteria(AuthTenantRoleOverride::userId eq userId))
}
