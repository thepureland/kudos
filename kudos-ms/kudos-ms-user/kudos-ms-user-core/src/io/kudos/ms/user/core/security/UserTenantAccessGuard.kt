package io.kudos.ms.user.core.security

import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.common.security.IPlatformAdministratorPolicy
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * What the current caller may see of user administration data.
 *
 * Every user row has one owner: its tenant for legacy rows, its customer organization for organization
 * rows (whose tenantId is empty). A caller is confined to its own owner.
 */
sealed interface UserAccessScope {

    /** Trusted internal work (no principal) or a verified platform administrator. */
    data object Unrestricted : UserAccessScope

    /** A legacy principal: confined to its tenant. */
    data class Tenant(val tenantId: String) : UserAccessScope

    /** An organization principal: confined to its organization, whichever tenant it is working in. */
    data class Organization(val organizationId: String) : UserAccessScope
}

/**
 * Mandatory owner boundary for user administration, independent of optional row-scope settings.
 * Empty context is reserved for internal/bootstrap work; HTTP administration must authenticate first.
 * Shared caches deliberately remain global: callers authorize before reading cached records.
 *
 * Legacy rows and principals behave exactly as before (owner = tenant). Organization principals reach
 * only organization-owned rows of their own organization; who among them may *write* is decided by the
 * organization member policy, not here.
 */
@Component
open class UserTenantAccessGuard {

    @Autowired(required = false)
    private var platformAdministratorPolicies: List<IPlatformAdministratorPolicy> = emptyList()

    @Autowired(required = false)
    private var ownership: IOrganizationOwnershipService? = null

    open fun hasPrincipal(): Boolean = CurrentUserKit.currentPrincipalOrNull() != null

    /** The current caller's scope. */
    open fun restrictedScope(): UserAccessScope {
        val principal = CurrentUserKit.currentPrincipalOrNull() ?: return UserAccessScope.Unrestricted
        if (platformAdministratorPolicies.any { it.isPlatformAdministrator(principal.id) }) return UserAccessScope.Unrestricted
        principal.organizationId?.takeIf(String::isNotBlank)?.let { return UserAccessScope.Organization(it) }
        require(principal.tenantId.isNotBlank()) { "Authenticated principal has no tenant" }
        return UserAccessScope.Tenant(principal.tenantId)
    }

    /**
     * Null denotes trusted internal work or a verified platform administrator. Organization principals
     * have no tenant restriction to offer: tenant-scoped (legacy) queries are closed to them.
     */
    open fun restrictedTenantId(): String? = when (val scope = restrictedScope()) {
        UserAccessScope.Unrestricted -> null
        is UserAccessScope.Tenant -> scope.tenantId
        is UserAccessScope.Organization -> throw IllegalArgumentException("Organization principals cannot query tenant-owned user data")
    }

    /** Legacy (tenant-owned) rows. */
    open fun assertCanAccess(tenantId: String) {
        if (!hasPrincipal()) return
        require(tenantId.isNotBlank()) { "Target tenant must not be blank" }
        when (val scope = restrictedScope()) {
            UserAccessScope.Unrestricted -> Unit
            is UserAccessScope.Tenant -> require(scope.tenantId == tenantId) { "Cross-tenant user administration is forbidden" }
            is UserAccessScope.Organization -> throw IllegalArgumentException("Cross-tenant user administration is forbidden")
        }
    }

    /** Organization-owned rows. */
    open fun assertCanAccessOrganization(organizationId: String) {
        require(organizationId.isNotBlank()) { "Target organization must not be blank" }
        when (val scope = restrictedScope()) {
            UserAccessScope.Unrestricted -> Unit
            is UserAccessScope.Tenant -> throw IllegalArgumentException("Cross-organization user administration is forbidden")
            is UserAccessScope.Organization -> require(scope.organizationId == organizationId) {
                "Cross-organization user administration is forbidden"
            }
        }
    }

    /**
     * Work done *in* a tenant (binding an external identity there, say) rather than on a row it owns:
     * a legacy principal in its own tenant, an organization principal in any tenant of its organization.
     */
    open fun assertCanWorkInTenant(tenantId: String) {
        val scope = restrictedScope()
        if (scope !is UserAccessScope.Organization) return assertCanAccess(tenantId)
        require(ownership?.organizationIdForTenant(tenantId) == scope.organizationId) {
            "Cross-organization user administration is forbidden"
        }
    }

    /** Whether [account] may be used in [tenantId]: its own tenant, or any tenant of its organization. */
    open fun accountServesTenant(accountTenantId: String, accountOrganizationId: String?, tenantId: String): Boolean =
        if (accountOrganizationId.isNullOrBlank()) accountTenantId == tenantId
        else ownership?.organizationIdForTenant(tenantId) == accountOrganizationId

    /** A row owned by [organizationId] when set, else by [tenantId]. */
    open fun assertCanAccessOwner(tenantId: String, organizationId: String?) {
        if (organizationId.isNullOrBlank()) assertCanAccess(tenantId) else assertCanAccessOrganization(organizationId)
    }

    /** A supplied tenant filter is checked before an omitted filter is filled from the principal. */
    open fun queryTenantId(requestedTenantId: String?): String? {
        requestedTenantId?.takeIf { it.isNotBlank() }?.let(::assertCanAccess)
        return restrictedTenantId() ?: requestedTenantId
    }

    companion object {

        /** The owner key of a row: organization when set, else tenant. Equal keys mean the same owner. */
        fun ownerKey(tenantId: String?, organizationId: String?): String =
            organizationId?.takeIf(String::isNotBlank)?.let { "ORG:$it" } ?: "TENANT:${tenantId.orEmpty()}"
    }
}
