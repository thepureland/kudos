package io.kudos.ms.auth.core.organization.service

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.organization.dao.AuthManagementRoleGrantDao
import io.kudos.ms.auth.core.organization.dao.AuthTenantRoleOverrideDao
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.event.SysTenantUpdated
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** One member's standing in a tenant, for opening / closing previews. */
data class TenantMemberImpact(
    val userId: String,
    val username: String?,
    val organizationAdmin: Boolean,
    val roleIds: List<String>,
)

/** What opening or closing a tenant changes (G-8). */
data class TenantOpeningPreview(
    val tenantId: String,
    val organizationId: String,
    val open: Boolean,
    val revision: Long,
    /** Opening: who will be able to enter, with their roles. Closing: who will lose entry. */
    val affectedMembers: List<TenantMemberImpact>,
    /** Opening: members who stay out (no effective role). */
    val excludedMembers: List<TenantMemberImpact>,
    /** Role id → number of affected members holding it. */
    val membersPerRole: Map<String, Int>,
)

/**
 * Tenant ownership and opening (G-2, G-8, G-9).
 *
 * The platform associates a tenant with an organization (it starts closed) and dissociates it (its
 * overrides and tenant permission administrator jurisdictions go with it; the audit keeps the record).
 * The organization's administrators and permission administrators open and close it, after a preview.
 * While closed nobody enters it, organization administrators included; overrides can still be prepared.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class OrganizationTenantService(
    private val mode: OrganizationMode,
    private val tenants: SysTenantDao,
    private val accounts: UserAccountDao,
    private val ownership: IOrganizationOwnershipService,
    private val overrides: AuthTenantRoleOverrideDao,
    private val managementRoles: AuthManagementRoleGrantDao,
    private val authority: OrganizationAuthority,
    private val policy: OrganizationAdministrationPolicy,
    private val effectiveRoles: EffectiveRoleResolver,
    private val recorder: OrganizationChangeRecorder,
    private val events: ApplicationEventPublisher,
) {

    open fun associate(tenantId: String, organizationId: String, reason: String?): Long {
        requirePlatform()
        val tenant = requireTenant(tenantId)
        require(!mode.isPlatformTenant(tenantId)) { "ORGANIZATION_PLATFORM_TENANT" }
        require(tenant.organizationId.isNullOrBlank()) { "ORGANIZATION_TENANT_ALREADY_ASSOCIATED" }
        ownership.requireActiveOrganization(organizationId)
        recorder.begin(organizationId)
        tenants.updateProperties(tenantId, mapOf(SysTenant::organizationId.name to organizationId, SysTenant::organizationOpen.name to false))
        events.publishEvent(SysTenantUpdated(tenantId))
        return recorder.commit(organizationId, "TENANT_ASSOCIATED", tenantId = tenantId, reason = reason)
    }

    open fun dissociate(tenantId: String, reason: String?): Long {
        requirePlatform()
        val tenant = requireTenant(tenantId)
        val organizationId = requireNotNull(tenant.organizationId?.takeIf(String::isNotBlank)) { "ORGANIZATION_TENANT_NOT_ASSOCIATED" }
        recorder.begin(organizationId)
        val removedOverrides = overrides.deleteByTenant(tenantId)
        val removedJurisdictions = managementRoles.deleteByTenant(tenantId)
        tenants.updateProperties(tenantId, mapOf(SysTenant::organizationId.name to null, SysTenant::organizationOpen.name to false))
        events.publishEvent(SysTenantUpdated(tenantId))
        return recorder.commit(
            organizationId, "TENANT_DISSOCIATED", tenantId = tenantId, reason = reason,
            detail = "overrides=$removedOverrides, tenantPermissionAdminJurisdictions=$removedJurisdictions",
        )
    }

    @Transactional(readOnly = true)
    open fun preview(tenantId: String, open: Boolean): TenantOpeningPreview {
        val tenant = requireTenant(tenantId)
        val organizationId = requireNotNull(tenant.organizationId?.takeIf(String::isNotBlank)) { "ORGANIZATION_TENANT_NOT_ASSOCIATED" }
        policy.assertCanOpenTenant(organizationId, tenantId)
        val members = accounts.search(Criteria(UserAccount::organizationId eq organizationId)).filter { it.active }
        val standings = members.map { account ->
            val admin = authority.isOrganizationAdmin(organizationId, account.id)
            val roles = effectiveRoles.effectiveRoleIds(organizationId, tenantId, account.id).sorted()
            TenantMemberImpact(account.id, account.username, admin, roles)
        }
        val entering = standings.filter { it.organizationAdmin || it.roleIds.isNotEmpty() }
        // Closing affects whoever could enter while open; opening affects whoever will be able to.
        val affected = if (open || tenant.organizationOpen) entering else emptyList()
        return TenantOpeningPreview(
            tenantId = tenantId,
            organizationId = organizationId,
            open = open,
            revision = recorder.current(organizationId),
            affectedMembers = affected,
            excludedMembers = if (open) standings - entering.toSet() else emptyList(),
            membersPerRole = affected.flatMap { it.roleIds }.groupingBy { it }.eachCount(),
        )
    }

    open fun setOpen(tenantId: String, open: Boolean, expectedRevision: Long?, reason: String?): Long {
        val tenant = requireTenant(tenantId)
        val organizationId = requireNotNull(tenant.organizationId?.takeIf(String::isNotBlank)) { "ORGANIZATION_TENANT_NOT_ASSOCIATED" }
        policy.assertCanOpenTenant(organizationId, tenantId)
        recorder.begin(organizationId, expectedRevision)
        if (tenant.organizationOpen == open) return recorder.current(organizationId)
        tenants.updateProperties(tenantId, mapOf(SysTenant::organizationOpen.name to open))
        events.publishEvent(SysTenantUpdated(tenantId))
        return recorder.commit(organizationId, if (open) "TENANT_OPENED" else "TENANT_CLOSED", tenantId = tenantId, reason = reason)
    }

    private fun requirePlatform() {
        mode.requireEnabled()
        val actor = authority.actorId()
        require(actor == null || authority.isPlatform(actor)) { "ORGANIZATION_PLATFORM_REQUIRED" }
    }

    private fun requireTenant(tenantId: String): SysTenant {
        mode.requireEnabled()
        return requireNotNull(tenants.get(tenantId)) { "ORGANIZATION_TENANT_NOT_FOUND" }
    }
}
