package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.organization.RoleOverrideAction
import io.kudos.ms.auth.core.organization.dao.AuthTenantRoleOverrideDao
import io.kudos.ms.auth.core.organization.model.po.AuthTenantRoleOverride
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.source.RoleSourceRegistry
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * One role as seen for one member in one tenant.
 *
 * @property default the role is one of the member's organization default roles
 * @property override the member's override of this role in this tenant, if any
 * @property applies the role applies to the member in this tenant
 * @property ineffective an override that does not change today's result (REMOVE of a non-default
 *   role, ADD of a default role); kept because it takes effect when the defaults change (G-3)
 */
data class MemberRoleView(
    val roleId: String,
    val default: Boolean,
    val override: RoleOverrideAction?,
    val applies: Boolean,
    val ineffective: Boolean,
)

/**
 * Effective business roles of an organization member in a tenant (G-1, G-3):
 *
 * > assigned(m, t) = (defaults(m) − REMOVE(m, t)) ∪ ADD(m, t)
 *
 * then expanded up the role hierarchy, deactivated roles dropped, and only roles of sub-systems the
 * tenant subscribes to kept. Defaults are the member's live direct grants on organization roles, the
 * roles of its active organization groups, and roles contributed by external role sources.
 *
 * Overrides act on assigned roles, before expansion: removing a role in a tenant does not remove what
 * the member inherits through another role. Management roles and the organization administrator
 * designation are not business roles and never appear here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class EffectiveRoleResolver(
    private val roles: AuthRoleDao,
    private val roleUsers: AuthRoleUserDao,
    private val groups: AuthGroupDao,
    private val groupUsers: AuthGroupUserDao,
    private val groupRoles: AuthGroupRoleDao,
    private val roleSources: RoleSourceRegistry,
    private val overrides: AuthTenantRoleOverrideDao,
    private val catalog: TenantSystemCatalog,
) {

    /** Business roles owned by [organizationId] (its roles keep the organization id as their owner). */
    open fun organizationRoles(organizationId: String, roleIds: Collection<String>): Map<String, AuthRole> =
        if (roleIds.isEmpty()) emptyMap()
        else roles.getByIds(roleIds.distinct()).filter { it.tenantId == organizationId }.associateBy { it.id }

    /** The member's organization default roles. */
    open fun defaultRoleIds(organizationId: String, userId: String, at: LocalDateTime = LocalDateTime.now()): Set<String> {
        val direct = roleUsers.searchRoleIdsByUserId(userId, at)
        val groupIds = groupUsers.searchGroupIdsByUserId(userId, at)
        val ownGroups = if (groupIds.isEmpty()) emptyList()
            else groups.getByIds(groupIds).filter { it.active && it.tenantId == organizationId }.map { it.id }
        val groupDerived = ownGroups.flatMap { groupRoles.searchRoleIdsByGroupId(it) }
        val external = roleSources.contributedRoleIds(userId)
        return organizationRoles(organizationId, direct + groupDerived + external).keys
    }

    /** The member's assigned roles in [tenantId], before hierarchy expansion. */
    open fun assignedRoleIds(
        organizationId: String,
        tenantId: String,
        userId: String,
        defaults: Set<String> = defaultRoleIds(organizationId, userId),
        tenantOverrides: List<AuthTenantRoleOverride> = overrides.searchByTenantAndUser(tenantId, userId),
    ): Set<String> {
        val removed = tenantOverrides.filter { it.action == RoleOverrideAction.REMOVE.name }.mapTo(hashSetOf()) { it.roleId }
        val added = tenantOverrides.filter { it.action == RoleOverrideAction.ADD.name }.map { it.roleId }
        return (defaults - removed) + organizationRoles(organizationId, added).keys
    }

    /**
     * The member's effective roles in [tenantId]: assigned roles expanded up the hierarchy, active only,
     * restricted to [subSystemCode] when given and always to the tenant's subscribed sub-systems.
     */
    open fun effectiveRoleIds(
        organizationId: String,
        tenantId: String,
        userId: String,
        subSystemCode: String? = null,
        assigned: Set<String> = assignedRoleIds(organizationId, tenantId, userId),
    ): Set<String> {
        if (assigned.isEmpty()) return emptySet()
        val expanded = assigned + roles.searchAncestorRoleIds(assigned)
        val active = roles.filterActiveRoleIds(expanded)
        val enabled = catalog.enabledSubSystemCodes(tenantId)
        return organizationRoles(organizationId, active).values
            .filter { it.subsysCode in enabled && (subSystemCode == null || it.subsysCode == subSystemCode) }
            .mapTo(linkedSetOf()) { it.id }
    }

    /** Every role that is a default or has an override for this member in [tenantId], with its status. */
    open fun memberRoleViews(organizationId: String, tenantId: String, userId: String): List<MemberRoleView> {
        val defaults = defaultRoleIds(organizationId, userId)
        val tenantOverrides = overrides.searchByTenantAndUser(tenantId, userId).associateBy { it.roleId }
        val assigned = assignedRoleIds(organizationId, tenantId, userId, defaults, tenantOverrides.values.toList())
        return (defaults + tenantOverrides.keys).sorted().map { roleId ->
            val action = tenantOverrides[roleId]?.action?.let(RoleOverrideAction::valueOf)
            MemberRoleView(
                roleId = roleId,
                default = roleId in defaults,
                override = action,
                applies = roleId in assigned,
                ineffective = (action == RoleOverrideAction.REMOVE && roleId !in defaults) ||
                    (action == RoleOverrideAction.ADD && roleId in defaults),
            )
        }
    }
}
