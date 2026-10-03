package io.kudos.ms.auth.core.organization.service

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.role.exclusion.dao.AuthRoleExclusionDao
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import org.springframework.stereotype.Component

/**
 * Duty separation in organization mode: an organization's SoD rules hold in every one of its tenants,
 * against the member's effective roles there (defaults and that tenant's overrides together).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationSodChecker(
    private val exclusions: AuthRoleExclusionDao,
    private val tenants: SysTenantDao,
    private val effectiveRoles: EffectiveRoleResolver,
) {

    /** "roleA:roleB" of the first organization rule both of whose roles are in [roleIds], or null. */
    open fun conflict(organizationId: String, roleIds: Set<String>): String? =
        exclusions.searchByRoleIds(roleIds)
            .firstOrNull { it.tenantId == organizationId && it.roleAId in roleIds && it.roleBId in roleIds }
            ?.let { "${it.roleAId}:${it.roleBId}" }

    /** Fails when [userId] would hold both roles of a rule in any of [tenantIds] (all tenants by default). */
    open fun assertNoConflict(organizationId: String, userId: String, tenantIds: Collection<String>? = null) {
        val targets = tenantIds ?: tenants.search(Criteria(SysTenant::organizationId eq organizationId)).map { it.id }
        for (tenantId in targets) {
            val effective = effectiveRoles.effectiveRoleIds(organizationId, tenantId, userId)
            conflict(organizationId, effective)?.let { throw IllegalArgumentException("AUTHZ_SOD_CONFLICT:$tenantId:$it") }
        }
    }
}
