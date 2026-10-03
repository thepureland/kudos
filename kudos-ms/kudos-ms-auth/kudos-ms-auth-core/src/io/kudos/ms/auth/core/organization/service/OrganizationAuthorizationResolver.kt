package io.kudos.ms.auth.core.organization.service

import io.kudos.base.query.Criteria
import io.kudos.base.query.inList
import io.kudos.ms.auth.common.authz.support.PermissionCodes
import io.kudos.ms.auth.common.authz.vo.PermissionGrantVo
import io.kudos.ms.auth.core.organization.OrganizationRank
import io.kudos.ms.auth.core.organization.init.OrganizationAuthorizationProperties
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import org.springframework.stereotype.Component

/** Authorization facts of one organization request, resolved from the authoritative tables. */
data class OrganizationAuthorization(
    val target: OrganizationRequestTarget,
    /** Tenant scope: whether the account may enter; organization scope: whether it may manage. */
    val allowed: Boolean,
    val denial: String?,
    val organizationAdmin: Boolean,
    val rank: OrganizationRank,
    /** Effective business roles (tenant scope; empty for the organization scope). */
    val roleIds: Set<String>,
    /** Grants of [roleIds]. Organization administrators are handled by [OrganizationAuthorizationResolver.permits]. */
    val grants: List<PermissionGrantVo>,
)

/**
 * Organization-mode authorization at run time. Nothing is cached: every protected request resolves
 * entry, roles and grants afresh, so a committed change (defaults, overrides, opening, designation)
 * applies to the next request without any invalidation message (first version; see the design).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationAuthorizationResolver(
    private val entries: TenantEntryService,
    private val effectiveRoles: EffectiveRoleResolver,
    private val authority: OrganizationAuthority,
    private val grantProjection: PermissionGrantsByUserIdCache,
    private val roleResources: AuthRoleResourceDao,
    private val resources: SysResourceDao,
    private val catalog: TenantSystemCatalog,
    private val properties: OrganizationAuthorizationProperties,
) {

    open fun resolve(target: OrganizationRequestTarget): OrganizationAuthorization = when (target) {
        is OrganizationRequestTarget.Tenant -> {
            val check = entries.check(target.tenantId, target.userId, target.subSystemCode)
            val admin = check.entry?.organizationAdmin == true
            val roleIds = if (!check.allowed || admin) emptySet()
                else effectiveRoles.effectiveRoleIds(target.organizationId, target.tenantId, target.userId, target.subSystemCode)
            OrganizationAuthorization(
                target, check.allowed, check.denial?.name, admin,
                if (admin) OrganizationRank.ORGANIZATION_ADMIN else OrganizationRank.MEMBER,
                roleIds, grantProjection.grantsOfRoles(roleIds),
            )
        }
        is OrganizationRequestTarget.Organization -> {
            val rank = authority.rank(target.organizationId, target.userId)
            val allowed = authority.hasOrganizationScope(target.organizationId, target.userId)
            OrganizationAuthorization(
                target, allowed, if (allowed) null else "NO_ORGANIZATION_SCOPE",
                rank == OrganizationRank.ORGANIZATION_ADMIN, rank, emptySet(), emptyList(),
            )
        }
    }

    /** Whether [code] is an organization management permission. */
    open fun isManagementCode(code: String): Boolean =
        properties.managementPermissionCodes.any { PermissionCodes.matches(it, code) }

    /**
     * The organization administrator's "everything" inside an open tenant (G-6), bounded by the
     * tenant's entitlement: management codes, plus codes of resources registered under a sub-system the
     * tenant subscribes to. Codes nobody registered (platform configuration, say) stay closed.
     */
    open fun permitsForOrganizationAdmin(tenantId: String, code: String): Boolean {
        if (isManagementCode(code)) return true
        val enabled = catalog.enabledSubSystemCodes(tenantId)
        if (enabled.isEmpty()) return false
        return resources.search(Criteria(SysResource::subSystemCode inList enabled))
            .any { PermissionCodes.matches(it.permissionCode, code) }
    }

    /** Resource ids visible to the request (menus): roles' resources, or everything entitled for an administrator. */
    open fun resourceIds(authorization: OrganizationAuthorization): Set<String> {
        if (!authorization.allowed) return emptySet()
        val target = authorization.target
        if (target !is OrganizationRequestTarget.Tenant) return emptySet()
        if (authorization.organizationAdmin) {
            val enabled = catalog.enabledSubSystemCodes(target.tenantId)
            if (enabled.isEmpty()) return emptySet()
            return resources.search(Criteria(SysResource::subSystemCode inList enabled))
                .filter { target.subSystemCode == null || it.subSystemCode == target.subSystemCode }
                .mapTo(linkedSetOf()) { it.id }
        }
        return roleResources.searchBindingsByRoleIds(authorization.roleIds).mapNotNullTo(linkedSetOf()) { it.resourceId?.takeIf(String::isNotBlank) }
    }

    /**
     * Permission codes of an administration-shaped authorization, for UIs: management codes, plus, for an
     * organization administrator in a tenant, the codes of the entitled resources.
     */
    open fun permissionCodesForAdministration(authorization: OrganizationAuthorization): Set<String> {
        val codes = linkedSetOf<String>()
        codes += properties.managementPermissionCodes
        val target = authorization.target
        if (authorization.organizationAdmin && target is OrganizationRequestTarget.Tenant) {
            val enabled = catalog.enabledSubSystemCodes(target.tenantId)
            if (enabled.isNotEmpty()) {
                resources.search(Criteria(SysResource::subSystemCode inList enabled))
                    .mapNotNullTo(codes) { it.permissionCode?.takeIf(String::isNotBlank) }
            }
        }
        return codes
    }
}
