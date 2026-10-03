package io.kudos.ms.user.core.org.service.impl

import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import io.kudos.ms.user.core.security.UserAccessScope
import io.kudos.ms.user.core.security.UserTenantAccessGuard
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService

/**
 * The checks every write to the shared directory goes through, in one place.
 *
 * Organization rows: organization mode must be on, the caller must reach the organization, and the
 * organization member policy (implemented by auth) must allow the action. Without a policy bean only
 * trusted internal work and platform administrators may write — never an organization member.
 *
 * Legacy rows: unchanged, except that organization mode admits them only in platform tenants (G-18).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationDirectoryGate(
    private val mode: OrganizationMode,
    private val access: UserTenantAccessGuard,
) {

    @Autowired(required = false)
    private var policy: IOrganizationMemberPolicy? = null

    @Autowired(required = false)
    private var ownership: IOrganizationOwnershipService? = null

    /** The organization owning [tenantId] in organization mode; null otherwise. */
    open fun organizationOfTenant(tenantId: String): String? =
        if (mode.enabled) ownership?.organizationIdForTenant(tenantId) else null

    open val organizationModeEnabled: Boolean get() = mode.enabled

    open fun assertCanManageDirectory(organizationId: String) {
        prepare(organizationId)
        policy?.assertCanManageDirectory(organizationId) ?: requireTrusted()
    }

    open fun assertCanManageMember(organizationId: String, userId: String?) {
        prepare(organizationId)
        policy?.assertCanManageMember(organizationId, userId) ?: requireTrusted()
    }

    open fun assertCanEndMembership(organizationId: String, userId: String) {
        prepare(organizationId)
        policy?.assertCanEndMembership(organizationId, userId) ?: requireTrusted()
    }

    /** Entry is an authorization fact of the auth module; without it no organization account signs in. */
    open fun canSignIn(tenantId: String, userId: String): Boolean =
        mode.enabled && policy?.canSignIn(tenantId, userId) == true

    /** Only trusted internal work or a platform administrator may create or disable organizations. */
    open fun assertPlatform() {
        mode.requireEnabled()
        requireTrusted()
    }

    /** Legacy tenant-owned rows exist in organization mode only inside platform tenants. */
    open fun assertLegacyTenantAllowed(tenantId: String?) {
        if (mode.enabled && !mode.isPlatformTenant(tenantId)) {
            throw IllegalArgumentException("ORGANIZATION_REQUIRED")
        }
    }

    private fun prepare(organizationId: String) {
        mode.requireEnabled()
        access.assertCanAccessOrganization(organizationId)
    }

    private fun requireTrusted() {
        require(access.restrictedScope() == UserAccessScope.Unrestricted) { "ORGANIZATION_MANAGEMENT_FORBIDDEN" }
    }
}
