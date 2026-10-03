package io.kudos.ms.auth.core.authentication.credential.service.impl

import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.user.core.account.dao.UserAccountDao
import org.springframework.stereotype.Component

/**
 * The owner id an account's credentials (password, history, TOTP, recovery codes, WebAuthn) are filed
 * under, in the tenant_id column of the credential tables.
 *
 * A legacy account's credentials belong to its tenant, exactly as before. An organization account has
 * one credential set for every tenant of its organization, filed under the organization id — whichever
 * of its tenants (or the organization scope) the operation comes from. A tenant of another
 * organization is refused.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class CredentialOwnerResolver(
    private val accounts: UserAccountDao,
    private val tenants: SysTenantDao,
) {

    open fun ownerOf(tenantId: String?, userId: String): String {
        val requested = tenantId.orEmpty()
        val organizationId = accounts.get(userId)?.organizationId?.takeIf(String::isNotBlank) ?: return requested
        require(requested.isBlank() || requested == organizationId || tenants.get(requested)?.organizationId == organizationId) {
            "CREDENTIAL_ORGANIZATION_MISMATCH"
        }
        return organizationId
    }

    /**
     * The owner credentials are looked up under when only the tenant is known (a WebAuthn credential
     * id presented at sign-in, say): the tenant's organization when it has one, else the tenant.
     */
    open fun ownerOfTenant(tenantId: String): String =
        tenants.get(tenantId)?.organizationId?.takeIf(String::isNotBlank) ?: tenantId

    /** Whether [userId] may use its credentials in [tenantId]: its own tenant, or any tenant of its organization. */
    open fun accountServes(tenantId: String, userId: String): Boolean {
        val account = accounts.get(userId) ?: return false
        val organizationId = account.organizationId?.takeIf(String::isNotBlank) ?: return account.tenantId == tenantId
        return tenantId == organizationId || tenants.get(tenantId)?.organizationId == organizationId
    }
}
