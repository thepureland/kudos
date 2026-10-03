package io.kudos.ms.user.core.org.service.iservice

import io.kudos.ms.user.core.org.model.po.UserOrg

/**
 * Authoritative ownership facts of organization mode. A tenant id is never substituted for an
 * organization id: legacy rows simply have no organization.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IOrganizationOwnershipService {

    /** The organization owning [tenantId]; null for platform, legacy or unassociated tenants. */
    fun organizationIdForTenant(tenantId: String): String?

    /** The organization owning account [userId]; null for legacy accounts. */
    fun organizationIdForAccount(userId: String): String?

    /** The customer organization root [organizationId], or null when it is not a root. */
    fun findOrganization(organizationId: String): UserOrg?

    /** The active customer organization root [organizationId]; fails otherwise. */
    fun requireActiveOrganization(organizationId: String): UserOrg
}
