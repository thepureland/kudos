package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.core.organization.init.OrganizationAuthorizationProperties
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.user.core.account.dao.UserAccountDao
import org.springframework.stereotype.Component

/** No scope of the organization is open to the account: no session. */
class OrganizationSessionDeniedException(code: String) : IllegalStateException(code)

/**
 * Decides which scope an organization account's new session works in.
 *
 * - The requested tenant, when the account may enter it (G-4, G-9).
 * - Otherwise the organization scope (no tenant), when the account is an organization administrator or
 *   holds a management role: they manage the organization even when they enter no tenant (G-6, G-7).
 * - Otherwise no session.
 *
 * Legacy accounts (and legacy mode) are returned unchanged.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationSessionTargeting(
    private val mode: OrganizationMode,
    private val accounts: UserAccountDao,
    private val entries: TenantEntryService,
    private val authority: OrganizationAuthority,
    private val properties: OrganizationAuthorizationProperties,
) {

    open fun resolve(context: AuthenticationContext): AuthenticationContext {
        if (!mode.enabled) return context
        val organizationId = accounts.get(context.userId)?.organizationId?.takeIf(String::isNotBlank) ?: return context
        require(context.organizationId == null || context.organizationId == organizationId) { "AUTHENTICATION_ORGANIZATION_MISMATCH" }
        val wantsOrganizationScope = context.tenantId.isBlank()
        if (!wantsOrganizationScope) {
            val entry = entries.check(context.tenantId, context.userId, context.subSystemCode)
            if (entry.allowed) return context.copy(organizationId = organizationId)
        }
        if (authority.hasOrganizationScope(organizationId, context.userId)) {
            return context.copy(organizationId = organizationId, tenantId = "", subSystemCode = properties.managementSubSystemCode)
        }
        throw OrganizationSessionDeniedException("AUTHENTICATION_TENANT_ACCESS_DENIED")
    }
}
