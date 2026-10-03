package io.kudos.ms.auth.common.organization.vo

/*
 * Request bodies of the organization-mode administration API (/api/admin/auth/organization/…).
 * Writes carry the organization revision the client last read; a stale one is refused with 409.
 */

/** Platform: associate a tenant with an organization (it starts closed). */
data class TenantAssociationRequest(val tenantId: String, val organizationId: String, val reason: String? = null)

/** Platform: dissociate a tenant from its organization. */
data class TenantDissociationRequest(val tenantId: String, val reason: String? = null)

/** Preview opening (`open = true`) or closing a tenant. */
data class TenantOpeningPreviewRequest(val tenantId: String, val open: Boolean)

/** Open or close a tenant. */
data class TenantOpeningRequest(val tenantId: String, val open: Boolean, val expectedRevision: Long? = null, val reason: String? = null)

/** One member of one organization. */
data class OrganizationMemberRequest(val organizationId: String, val userId: String)

/** Preview or save a member's direct default roles. */
data class MemberDefaultRolesRequest(
    val organizationId: String,
    val userId: String,
    val roleIds: List<String>,
    val expectedRevision: Long? = null,
    val reason: String? = null,
)

/** Set (`action` ADD / REMOVE) or remove (no action needed) a member's override of a role in a tenant. */
data class MemberRoleOverrideRequest(
    val organizationId: String,
    val tenantId: String,
    val userId: String,
    val roleId: String,
    val action: String? = null,
    val expectedRevision: Long? = null,
    val reason: String? = null,
)

/** One organization. */
data class OrganizationRequest(val organizationId: String)

/** Designate or revoke an organization administrator. */
data class OrganizationAdminDesignationRequest(val organizationId: String, val userId: String, val reason: String? = null)

/** Grant or revoke a management role; tenant permission administrators name the tenants they govern. */
data class ManagementRoleRequest(
    val organizationId: String,
    val userId: String,
    val roleKind: String,
    val tenantIds: List<String> = emptyList(),
    val reason: String? = null,
)
