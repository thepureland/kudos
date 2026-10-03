package io.kudos.ms.auth.core.policy

import io.kudos.ms.auth.core.organization.service.OrganizationAdministrationPolicy
import io.kudos.ms.auth.core.platform.authz.PlatformAdministratorPolicy
import io.kudos.ms.auth.core.principal.PrincipalDirectoryRegistry
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * Enforces the owner boundary on authorization administration write paths: the tenant for legacy
 * roles and groups, the customer organization for organization ones (organization mode), where the
 * organization administration policy decides.
 *
 * @author K
 * @author AI: Codex
 */
@Component
open class TenantAdministrationGuard {

    @Resource
    private lateinit var platformAdministratorPolicy: PlatformAdministratorPolicy

    @Resource
    private lateinit var principalDirectoryRegistry: PrincipalDirectoryRegistry

    /** Organization-mode rules; absent in pure unit tests, where only legacy owners exist. */
    @Autowired(required = false)
    @Lazy
    private var organizationPolicy: OrganizationAdministrationPolicy? = null

    @Autowired(required = false)
    private var organizationOwnership: IOrganizationOwnershipService? = null

    @Autowired(required = false)
    private var organizationMode: OrganizationMode? = null

    /**
     * Whether [ownerId] (a role's, group's or SoD rule's tenantId) names a customer organization rather
     * than a tenant. Organization-owned rows keep the organization id as their owner.
     */
    open fun isOrganizationOwner(ownerId: String): Boolean =
        organizationMode?.enabled == true && organizationOwnership?.findOrganization(ownerId) != null

    private fun policy(): OrganizationAdministrationPolicy =
        requireNotNull(organizationPolicy) { "ORGANIZATION_MODE_UNAVAILABLE" }

    /**
     * Empty context is reserved for trusted internal/bootstrap calls. An authenticated caller may
     * manage its own tenant only, unless it satisfies the hardened platform-administrator policy.
     */
    open fun assertCanManage(targetTenantId: String) {
        require(targetTenantId.isNotBlank()) { "target tenant id must not be blank." }
        if (isOrganizationOwner(targetTenantId)) return policy().assertManager(targetTenantId)
        val current = CurrentUserKit.currentPrincipalOrNull() ?: return
        require(current.tenantId == targetTenantId || platformAdministratorPolicy.isPlatformAdministrator(current.id)) {
            "cross-tenant authorization administration is forbidden: caller tenant=${current.tenantId}, target=$targetTenantId."
        }
    }

    /** Platform administrator role codes are deployment-reserved and cannot be minted by tenants. */
    open fun assertRoleCodeManageable(code: String, tenantId: String) {
        assertCanManage(tenantId)
        val current = CurrentUserKit.currentPrincipalOrNull() ?: return
        require(!platformAdministratorPolicy.isReservedRoleCode(code) ||
            platformAdministratorPolicy.isPlatformAdministrator(current.id)) {
            "role code $code is reserved for platform administration."
        }
    }

    /** A group membership may never cross the group's tenant boundary. */
    open fun assertPrincipalBelongsToTenant(principalId: String, principalType: String?, tenantId: String) {
        val principal = principalDirectoryRegistry.find(principalId, principalType)
            ?: throw IllegalArgumentException("principal not found: $principalId")
        require(principal.active) { "principal $principalId is inactive." }
        // An organization account's owner is its organization (see PrincipalFacts.tenantId), so the
        // same comparison keeps organization groups to organization members.
        require(principal.tenantId == tenantId) {
            "principal $principalId belongs to tenant ${principal.tenantId}, not $tenantId."
        }
    }

    /**
     * Whether a create request names (or, through the caller, implies) an organization owner; when it
     * does not, the legacy path is taken unchanged.
     */
    open fun requestsOrganizationOwner(tenantId: String?, organizationId: String?): Boolean =
        !organizationId.isNullOrBlank() ||
            (!tenantId.isNullOrBlank() && isOrganizationOwner(tenantId)) ||
            (tenantId.isNullOrBlank() && callerOrganization() != null) ||
            (organizationMode?.enabled == true && !tenantId.isNullOrBlank())

    /**
     * The owner a new role or group is created for. An explicit organization wins; a tenant id that names
     * an organization means that organization; with neither, an organization principal works for its own
     * organization. A plain tenant is a legacy owner, which organization mode admits only for platform
     * tenants.
     */
    open fun resolveOwner(tenantId: String?, organizationId: String?): AdministrationOwner {
        val requestedOrganization = organizationId?.takeIf(String::isNotBlank)
        val requestedTenant = tenantId?.takeIf(String::isNotBlank)
        if (requestedOrganization != null) {
            require(requestedTenant == null || requestedTenant == requestedOrganization) { "tenantId and organizationId disagree." }
            require(isOrganizationOwner(requestedOrganization)) { "ORGANIZATION_NOT_FOUND" }
            return AdministrationOwner(requestedOrganization, requestedOrganization)
        }
        if (requestedTenant != null) {
            if (isOrganizationOwner(requestedTenant)) return AdministrationOwner(requestedTenant, requestedTenant)
            if (organizationMode?.enabled == true) {
                require(organizationMode?.isPlatformTenant(requestedTenant) == true) { "ORGANIZATION_REQUIRED" }
            }
            return AdministrationOwner(requestedTenant, null)
        }
        val principalOrganization = CurrentUserKit.currentPrincipalOrNull()?.organizationId?.takeIf(String::isNotBlank)
        return if (principalOrganization != null && isOrganizationOwner(principalOrganization)) {
            AdministrationOwner(principalOrganization, principalOrganization)
        } else AdministrationOwner(null, null)
    }

    /**
     * The organization an organization principal is confined to when listing roles, groups or SoD rules;
     * null for legacy principals, platform administrators and trusted internal work.
     */
    open fun callerOrganization(): String? {
        if (organizationMode?.enabled != true) return null
        val current = CurrentUserKit.currentPrincipalOrNull() ?: return null
        val organizationId = current.organizationId?.takeIf(String::isNotBlank) ?: return null
        return organizationId.takeUnless { platformAdministratorPolicy.isPlatformAdministrator(current.id) }
    }

    /**
     * Changing a role's definition. Organization roles additionally forbid an organization permission
     * administrator from editing a role it holds (G-12).
     */
    open fun assertCanEditRole(roleId: String, ownerId: String) {
        assertCanManage(ownerId)
        if (isOrganizationOwner(ownerId)) policy().assertCanEditRoleDefinition(roleId)
    }

    /**
     * Changing which roles [principalId] holds by default (a direct grant, a group membership). For
     * organization owners nobody changes their own defaults.
     */
    open fun assertCanChangeHolder(ownerId: String, principalId: String, removal: Boolean = false) {
        if (isOrganizationOwner(ownerId)) policy().assertCanChangeDefaults(ownerId, principalId, removal)
    }
}

/**
 * Owner of a role, group or SoD rule. [ownerId] goes into the row's tenantId; [organizationId] is set
 * (to the same id) for organization-owned rows.
 */
data class AdministrationOwner(val ownerId: String?, val organizationId: String?)
