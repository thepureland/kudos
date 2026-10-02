package io.kudos.ms.user.core.security

import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.common.security.IPlatformAdministratorPolicy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Mandatory tenant boundary for user administration, independent of optional row-scope settings.
 * Empty context is reserved for internal/bootstrap work; HTTP administration must authenticate first.
 * Shared caches deliberately remain global: callers authorize before reading cached records.
 */
@Component
open class UserTenantAccessGuard {

    @Autowired(required = false)
    private var platformAdministratorPolicies: List<IPlatformAdministratorPolicy> = emptyList()

    open fun hasPrincipal(): Boolean = CurrentUserKit.currentPrincipalOrNull() != null

    /** Null denotes trusted internal work or a verified platform administrator. */
    open fun restrictedTenantId(): String? {
        val principal = CurrentUserKit.currentPrincipalOrNull() ?: return null
        require(principal.tenantId.isNotBlank()) { "Authenticated principal has no tenant" }
        return if (platformAdministratorPolicies.any { it.isPlatformAdministrator(principal.id) }) null
        else principal.tenantId
    }

    open fun assertCanAccess(tenantId: String) {
        if (!hasPrincipal()) return
        require(tenantId.isNotBlank()) { "Target tenant must not be blank" }
        val restricted = restrictedTenantId()
        require(restricted == null || restricted == tenantId) { "Cross-tenant user administration is forbidden" }
    }

    /** A supplied tenant filter is checked before an omitted filter is filled from the principal. */
    open fun queryTenantId(requestedTenantId: String?): String? {
        requestedTenantId?.takeIf { it.isNotBlank() }?.let(::assertCanAccess)
        return restrictedTenantId() ?: requestedTenantId
    }
}
