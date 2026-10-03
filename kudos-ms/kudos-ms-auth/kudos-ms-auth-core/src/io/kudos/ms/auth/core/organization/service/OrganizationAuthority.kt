package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.core.organization.ManagementRoleKind
import io.kudos.ms.auth.core.organization.OrganizationRank
import io.kudos.ms.auth.core.organization.dao.AuthManagementRoleGrantDao
import io.kudos.ms.auth.core.organization.dao.AuthOrganizationAdminDao
import io.kudos.ms.auth.core.platform.authz.PlatformAdministratorPolicy
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.dao.UserAccountDao
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * Who someone is towards one organization: platform, organization administrator, one of the two
 * built-in management roles, plain member, or outsider. Read from the authoritative tables on every
 * call; nothing here is cached.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationAuthority(
    private val admins: AuthOrganizationAdminDao,
    private val managementRoles: AuthManagementRoleGrantDao,
    private val accounts: UserAccountDao,
    @param:Lazy private val platform: PlatformAdministratorPolicy,
) {

    /** The authenticated actor, or null for trusted internal work. */
    open fun actorId(): String? = CurrentUserKit.currentUserIdOrNull()

    open fun isPlatform(userId: String?): Boolean = userId != null && platform.isPlatformAdministrator(userId)

    /** Whether the account belongs to [organizationId] and is active. */
    open fun isMember(organizationId: String, userId: String): Boolean =
        accounts.get(userId)?.let { it.organizationId == organizationId && it.active } == true

    open fun isOrganizationAdmin(organizationId: String, userId: String): Boolean =
        admins.find(organizationId, userId) != null && isMember(organizationId, userId)

    open fun organizationAdminIds(organizationId: String): List<String> =
        admins.searchByOrganization(organizationId).map { it.userId }.filter { isMember(organizationId, it) }

    open fun hasManagementRole(organizationId: String, userId: String, kind: ManagementRoleKind): Boolean =
        managementRoles.searchByUser(organizationId, userId).any { it.roleKind == kind.name } && isMember(organizationId, userId)

    /** Tenants governed by [userId] as tenant permission administrator. */
    open fun governedTenantIds(organizationId: String, userId: String): Set<String> =
        if (!isMember(organizationId, userId)) emptySet()
        else managementRoles.searchByUser(organizationId, userId)
            .filter { it.roleKind == ManagementRoleKind.TENANT_PERMISSION_ADMIN.name }
            .mapTo(linkedSetOf()) { it.tenantId }

    /** The highest rank [userId] holds towards [organizationId]. */
    open fun rank(organizationId: String, userId: String?): OrganizationRank {
        if (userId == null) return OrganizationRank.NONE
        if (isPlatform(userId)) return OrganizationRank.PLATFORM
        if (!isMember(organizationId, userId)) return OrganizationRank.NONE
        if (admins.find(organizationId, userId) != null) return OrganizationRank.ORGANIZATION_ADMIN
        val kinds = managementRoles.searchByUser(organizationId, userId).map { it.roleKind }.toSet()
        return when {
            ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN.name in kinds -> OrganizationRank.ORGANIZATION_PERMISSION_ADMIN
            ManagementRoleKind.TENANT_PERMISSION_ADMIN.name in kinds -> OrganizationRank.TENANT_PERMISSION_ADMIN
            else -> OrganizationRank.MEMBER
        }
    }

    /** Whether [userId] may work in the ORGANIZATION scope: an administrator or a management role holder. */
    open fun hasOrganizationScope(organizationId: String, userId: String): Boolean =
        rank(organizationId, userId).let { it != OrganizationRank.MEMBER && it != OrganizationRank.NONE && it != OrganizationRank.PLATFORM }
}
