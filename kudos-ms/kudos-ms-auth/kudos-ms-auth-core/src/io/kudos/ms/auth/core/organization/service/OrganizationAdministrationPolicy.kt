package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.core.organization.ManagementRoleKind
import io.kudos.ms.auth.core.organization.OrganizationRank
import io.kudos.ms.auth.core.organization.OrganizationRank.NONE
import io.kudos.ms.auth.core.organization.OrganizationRank.ORGANIZATION_ADMIN
import io.kudos.ms.auth.core.organization.OrganizationRank.ORGANIZATION_PERMISSION_ADMIN
import io.kudos.ms.auth.core.organization.OrganizationRank.PLATFORM
import io.kudos.ms.auth.core.organization.OrganizationRank.TENANT_PERMISSION_ADMIN
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import org.springframework.stereotype.Component
import io.kudos.ms.user.core.account.dao.UserAccountDao

/** A management action the caller may not take; the message is a stable error code. */
class OrganizationManagementException(code: String) : IllegalArgumentException(code)

/**
 * Every "who may manage what" rule of organization mode, in one place (G-6 … G-12, G-16, G-17).
 *
 * Ranks: platform > organization administrator > organization permission administrator > tenant
 * permission administrator > member. Trusted internal work (no principal) passes like the platform.
 *
 * - Directory, members, role definitions, groups, defaults, every tenant's overrides, tenant opening:
 *   organization administrator or organization permission administrator.
 * - A tenant's overrides: additionally the tenant permission administrators governing that tenant.
 * - Organization administrators: designated and revoked by the platform or another organization
 *   administrator; never oneself, never the last one.
 * - Management roles: granted and revoked only from a strictly higher rank (organization administrator
 *   for both kinds, organization permission administrator for tenant permission administrators only).
 * - Nobody changes their own defaults, overrides, management roles or designation; an organization
 *   permission administrator does not change the definition of a role it holds.
 * - Ending a membership: only from a strictly higher rank; never the last organization administrator,
 *   except by the platform (emergency stop, G-16).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationAdministrationPolicy(
    private val mode: OrganizationMode,
    private val authority: OrganizationAuthority,
    private val roles: AuthRoleDao,
    private val tenants: SysTenantDao,
    private val effectiveRoles: EffectiveRoleResolver,
    private val entries: TenantEntryService,
    private val recorder: OrganizationChangeRecorder,
    private val accounts: UserAccountDao,
) : IOrganizationMemberPolicy {

    /** The caller's rank towards [organizationId]; trusted internal work counts as the platform. */
    open fun actorRank(organizationId: String): OrganizationRank =
        authority.actorId()?.let { authority.rank(organizationId, it) } ?: PLATFORM

    // region directory and members (user module port)

    override fun assertCanManageDirectory(organizationId: String) = assertManager(organizationId)

    override fun assertCanManageMember(organizationId: String, userId: String?) {
        assertManager(organizationId)
        // Editing an account that outranks you would let you take it over (a password, an email).
        if (userId != null && userId != authority.actorId()) assertOutranks(organizationId, userId)
    }

    override fun assertCanEndMembership(organizationId: String, userId: String) {
        mode.requireEnabled()
        // Serialized with designations: two administrators ending each other cannot both see a successor.
        recorder.begin(organizationId)
        val rank = actorRank(organizationId)
        if (rank == PLATFORM) return  // includes the emergency stop of the last administrator (G-16)
        assertManager(organizationId)
        deny(userId == authority.actorId(), "ORGANIZATION_CANNOT_END_OWN_MEMBERSHIP")
        assertOutranks(organizationId, userId)
        val admins = authority.organizationAdminIds(organizationId)
        deny(userId in admins && admins.size <= 1, "ORGANIZATION_LAST_ADMIN")
    }

    override fun canSignIn(tenantId: String, userId: String): Boolean {
        if (entries.check(tenantId, userId).allowed) return true
        val organizationId = tenants.get(tenantId)?.organizationId ?: return false
        return authority.hasOrganizationScope(organizationId, userId)
    }

    // endregion

    // region authorization configuration

    /** Organization-wide configuration: directory, role definitions, groups, defaults, opening tenants. */
    open fun assertManager(organizationId: String) {
        mode.requireEnabled()
        deny(actorRank(organizationId) !in MANAGERS, "ORGANIZATION_MANAGEMENT_FORBIDDEN")
    }

    /** Changing a role definition (permissions, data scope, hierarchy, activity, deletion). */
    open fun assertCanEditRoleDefinition(roleId: String) {
        val role = roles.get(roleId) ?: return
        val organizationId = role.organizationId ?: return
        assertManager(organizationId)
        if (actorRank(organizationId) != ORGANIZATION_PERMISSION_ADMIN) return
        val actor = authority.actorId() ?: return
        deny(holdsRole(organizationId, actor, roleId), "ORGANIZATION_CANNOT_EDIT_HELD_ROLE")
    }

    /**
     * Changing [userId]'s organization default roles (direct grants, group memberships). A [removal]
     * may also concern an account that is no longer active (offboarding clean-up).
     */
    open fun assertCanChangeDefaults(organizationId: String, userId: String, removal: Boolean = false) {
        assertManager(organizationId)
        assertNotSelf(userId)
        if (removal) {
            deny(accounts.get(userId)?.organizationId != organizationId, "ORGANIZATION_MEMBER_NOT_FOUND")
        } else {
            requireMember(organizationId, userId)
        }
    }

    /** Changing [userId]'s overrides in [tenantId]. */
    open fun assertCanChangeOverrides(organizationId: String, tenantId: String, userId: String) {
        mode.requireEnabled()
        requireTenantOf(organizationId, tenantId)
        val rank = actorRank(organizationId)
        val governs = rank == TENANT_PERMISSION_ADMIN &&
            tenantId in authority.governedTenantIds(organizationId, requireNotNull(authority.actorId()))
        deny(rank !in MANAGERS && !governs, "ORGANIZATION_MANAGEMENT_FORBIDDEN")
        assertNotSelf(userId)
        requireMember(organizationId, userId)
        // A tenant permission administrator handles ordinary members only, not other managers.
        if (rank == TENANT_PERMISSION_ADMIN) assertOutranks(organizationId, userId)
    }

    /** Opening, closing (G-8). */
    open fun assertCanOpenTenant(organizationId: String, tenantId: String) {
        assertManager(organizationId)
        requireTenantOf(organizationId, tenantId)
    }

    /** Designating or revoking an organization administrator (G-10). */
    open fun assertCanDesignateAdmin(organizationId: String, userId: String) {
        mode.requireEnabled()
        deny(actorRank(organizationId) !in setOf(PLATFORM, ORGANIZATION_ADMIN), "ORGANIZATION_ADMIN_DESIGNATION_FORBIDDEN")
        assertNotSelf(userId)
        requireMember(organizationId, userId)
    }

    /** Granting or revoking a management role (G-11). */
    open fun assertCanGrantManagementRole(organizationId: String, userId: String, kind: ManagementRoleKind) {
        mode.requireEnabled()
        val allowed = when (kind) {
            ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN -> setOf(PLATFORM, ORGANIZATION_ADMIN)
            ManagementRoleKind.TENANT_PERMISSION_ADMIN -> MANAGERS
        }
        deny(actorRank(organizationId) !in allowed, "ORGANIZATION_MANAGEMENT_ROLE_GRANT_FORBIDDEN")
        assertNotSelf(userId)
        requireMember(organizationId, userId)
        assertOutranks(organizationId, userId)
    }

    /** Organization-scope reads: any management rank. */
    open fun assertCanReadOrganization(organizationId: String) {
        mode.requireEnabled()
        deny(actorRank(organizationId) !in MANAGERS + TENANT_PERMISSION_ADMIN, "ORGANIZATION_MANAGEMENT_FORBIDDEN")
    }

    /** Tenants the caller may manage overrides in. */
    open fun manageableTenantIds(organizationId: String): Set<String>? = when (actorRank(organizationId)) {
        in MANAGERS -> null  // all
        TENANT_PERMISSION_ADMIN -> authority.governedTenantIds(organizationId, requireNotNull(authority.actorId()))
        else -> emptySet()
    }

    // endregion

    /**
     * Whether [userId] currently holds [roleId] — by default or in any tenant, directly or as an ancestor
     * of a role it holds (an ancestor's permissions flow down to it).
     */
    open fun holdsRole(organizationId: String, userId: String, roleId: String): Boolean {
        fun withAncestors(assigned: Set<String>) = assigned + roles.searchAncestorRoleIds(assigned)
        val defaults = effectiveRoles.defaultRoleIds(organizationId, userId)
        if (roleId in withAncestors(defaults)) return true
        return tenants.allSearch().filter { it.organizationId == organizationId }.any { tenant ->
            roleId in withAncestors(effectiveRoles.assignedRoleIds(organizationId, tenant.id, userId, defaults))
        }
    }

    private fun assertNotSelf(userId: String) = deny(userId == authority.actorId(), "ORGANIZATION_CANNOT_CHANGE_OWN_AUTHORIZATION")

    private fun assertOutranks(organizationId: String, userId: String) {
        val actor = actorRank(organizationId)
        if (actor == PLATFORM) return
        val target = authority.rank(organizationId, userId)
        // Organization administrators manage each other through designation; everybody else only manages below.
        val ok = if (actor == ORGANIZATION_ADMIN) target != PLATFORM else actor.outranks(target)
        deny(!ok, "ORGANIZATION_TARGET_OUTRANKS_ACTOR")
    }

    private fun requireMember(organizationId: String, userId: String) =
        deny(authority.rank(organizationId, userId).let { it == NONE || it == PLATFORM }, "ORGANIZATION_MEMBER_NOT_FOUND")

    private fun requireTenantOf(organizationId: String, tenantId: String) =
        deny(tenants.get(tenantId)?.organizationId != organizationId, "ORGANIZATION_TENANT_MISMATCH")

    private fun deny(condition: Boolean, code: String) {
        if (condition) throw OrganizationManagementException(code)
    }

    private companion object {
        val MANAGERS = setOf(PLATFORM, ORGANIZATION_ADMIN, ORGANIZATION_PERMISSION_ADMIN)
    }
}
