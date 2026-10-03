package io.kudos.ms.auth.core.organization.service

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.organization.ManagementRoleKind
import io.kudos.ms.auth.core.organization.RoleOverrideAction
import io.kudos.ms.auth.core.organization.dao.AuthManagementRoleGrantDao
import io.kudos.ms.auth.core.organization.dao.AuthTenantRoleOverrideDao
import io.kudos.ms.auth.core.organization.model.po.AuthTenantRoleOverride
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.dao.UserAccountDao
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** An organization business role, for pickers and labels. */
data class OrganizationRoleRow(val id: String, val code: String, val name: String, val subSystemCode: String, val active: Boolean)

/** A member's roles in one tenant of the organization. */
data class MemberTenantView(
    val tenantId: String,
    val tenantName: String,
    val open: Boolean,
    /** Whether the member may enter now; [entryDenial] says why not. */
    val canEnter: Boolean,
    val entryDenial: String?,
    /** Whether the caller may change this member's overrides here. */
    val editable: Boolean,
    val roles: List<MemberRoleView>,
)

/** Everything the console shows about one member's authorization. */
data class MemberAuthorizationView(
    val organizationId: String,
    val userId: String,
    val username: String?,
    val revision: Long,
    val organizationAdmin: Boolean,
    val managementRoles: List<String>,
    /** The member's direct default grants; editable through [OrganizationMemberRoleService.saveDefaults]. */
    val directDefaultRoleIds: List<String>,
    /** All default roles, including those relayed by groups and external sources. */
    val defaultRoleIds: List<String>,
    val defaultsEditable: Boolean,
    val tenants: List<MemberTenantView>,
    val roleCatalog: List<OrganizationRoleRow>,
)

/** A tenant whose entry would change with a defaults edit (the G-4 risk made visible). */
data class EntryChange(val tenantId: String, val tenantName: String, val regains: Boolean)

/** Preview of a defaults edit. */
data class DefaultRolesPreview(
    val revision: Long,
    val addedRoleIds: List<String>,
    val removedRoleIds: List<String>,
    /** Tenants where the member gains or loses entry (regains = true: previously all roles removed). */
    val entryChanges: List<EntryChange>,
    /** Tenants where a role appears or disappears, per tenant. */
    val tenantRoleChanges: Map<String, List<String>>,
    val sodConflicts: List<String>,
)

/**
 * A member's organization default roles and per-tenant overrides (G-1, G-3, G-4).
 *
 * Defaults are direct grants on organization roles (plus group-relayed and external roles, which are
 * edited where they come from). Overrides add or remove one role for one member in one tenant. Every
 * write locks the organization's revision, checks the caller's expected revision, checks duty
 * separation in every tenant the change touches, and is audited.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class OrganizationMemberRoleService(
    private val tenants: SysTenantDao,
    private val accounts: UserAccountDao,
    private val roles: AuthRoleDao,
    private val roleUsers: AuthRoleUserDao,
    private val roleUserService: IAuthRoleUserService,
    private val sod: OrganizationSodChecker,
    private val overrides: AuthTenantRoleOverrideDao,
    private val managementRoles: AuthManagementRoleGrantDao,
    private val authority: OrganizationAuthority,
    private val policy: OrganizationAdministrationPolicy,
    private val effectiveRoles: EffectiveRoleResolver,
    private val entries: TenantEntryService,
    private val recorder: OrganizationChangeRecorder,
) {

    @Transactional(readOnly = true)
    open fun roleCatalog(organizationId: String): List<OrganizationRoleRow> {
        policy.assertCanReadOrganization(organizationId)
        return catalog(organizationId)
    }

    @Transactional(readOnly = true)
    open fun read(organizationId: String, userId: String): MemberAuthorizationView {
        policy.assertCanReadOrganization(organizationId)
        val account = requireMember(organizationId, userId)
        val manageable = policy.manageableTenantIds(organizationId)
        val actor = CurrentUserKit.currentUserIdOrNull()
        val tenantViews = organizationTenants(organizationId).map { tenant ->
            val entry = entries.check(tenant.id, userId)
            MemberTenantView(
                tenantId = tenant.id,
                tenantName = tenant.name,
                open = tenant.organizationOpen,
                canEnter = entry.allowed,
                entryDenial = entry.denial?.name,
                editable = userId != actor && (manageable == null || tenant.id in manageable),
                roles = effectiveRoles.memberRoleViews(organizationId, tenant.id, userId),
            )
        }
        return MemberAuthorizationView(
            organizationId = organizationId,
            userId = userId,
            username = account.username,
            revision = recorder.current(organizationId),
            organizationAdmin = authority.isOrganizationAdmin(organizationId, userId),
            managementRoles = managementRoles.searchByUser(organizationId, userId)
                .map { if (it.roleKind == ManagementRoleKind.TENANT_PERMISSION_ADMIN.name) "${it.roleKind}:${it.tenantId}" else it.roleKind },
            directDefaultRoleIds = directDefaults(organizationId, userId).sorted(),
            defaultRoleIds = effectiveRoles.defaultRoleIds(organizationId, userId).sorted(),
            defaultsEditable = userId != actor && manageable == null,
            tenants = tenantViews,
            roleCatalog = catalog(organizationId),
        )
    }

    @Transactional(readOnly = true)
    open fun previewDefaults(organizationId: String, userId: String, directRoleIds: Collection<String>): DefaultRolesPreview {
        policy.assertCanChangeDefaults(organizationId, userId)
        return computePreview(organizationId, userId, requireOrganizationRoles(organizationId, directRoleIds))
    }

    /** Replaces the member's direct default grants with [directRoleIds]. */
    open fun saveDefaults(organizationId: String, userId: String, directRoleIds: Collection<String>, expectedRevision: Long?, reason: String?): Long {
        policy.assertCanChangeDefaults(organizationId, userId)
        val target = requireOrganizationRoles(organizationId, directRoleIds)
        recorder.begin(organizationId, expectedRevision)
        val current = directDefaults(organizationId, userId)
        (target - current).forEach { roleUserService.batchBind(it, listOf(userId)) }
        (current - target).forEach { roleUserService.unbind(it, userId) }
        assertNoSodConflict(organizationId, userId, organizationTenants(organizationId).map { it.id })
        return recorder.commit(
            organizationId, "MEMBER_DEFAULTS_CHANGED", targetUserId = userId, reason = reason,
            detail = "added=${(target - current).sorted()}, removed=${(current - target).sorted()}",
        )
    }

    /** Sets the member's override of [roleId] in [tenantId] to [action], replacing any opposite one. */
    open fun saveOverride(
        organizationId: String, tenantId: String, userId: String, roleId: String,
        action: RoleOverrideAction, expectedRevision: Long?, reason: String?,
    ): Long {
        policy.assertCanChangeOverrides(organizationId, tenantId, userId)
        requireOrganizationRoles(organizationId, listOf(roleId))
        recorder.begin(organizationId, expectedRevision)
        val existing = overrides.find(tenantId, userId, roleId)
        if (existing?.action == action.name) return recorder.current(organizationId)
        existing?.let { overrides.deleteById(it.id) }
        overrides.insert(AuthTenantRoleOverride {
            this.organizationId = organizationId
            this.tenantId = tenantId
            this.userId = userId
            this.roleId = roleId
            this.action = action.name
            this.reason = reason?.take(512)
            this.createUserId = CurrentUserKit.currentUserIdOrNull()
            this.createTime = LocalDateTime.now()
        })
        assertNoSodConflict(organizationId, userId, listOf(tenantId))
        return recorder.commit(
            organizationId, "OVERRIDE_${action.name}", tenantId = tenantId, targetUserId = userId, targetId = roleId,
            reason = reason, detail = existing?.let { "replaced ${it.action}" },
        )
    }

    /** Removes the member's override of [roleId] in [tenantId]: the role follows the defaults again. */
    open fun removeOverride(organizationId: String, tenantId: String, userId: String, roleId: String, expectedRevision: Long?, reason: String?): Long {
        policy.assertCanChangeOverrides(organizationId, tenantId, userId)
        recorder.begin(organizationId, expectedRevision)
        val existing = overrides.find(tenantId, userId, roleId) ?: return recorder.current(organizationId)
        overrides.deleteById(existing.id)
        // Restoring inheritance can widen access as easily as narrow it, so it is checked like any write.
        assertNoSodConflict(organizationId, userId, listOf(tenantId))
        return recorder.commit(
            organizationId, "OVERRIDE_RESET", tenantId = tenantId, targetUserId = userId, targetId = roleId,
            reason = reason, detail = "removed ${existing.action}",
        )
    }

    private fun computePreview(organizationId: String, userId: String, target: Set<String>): DefaultRolesPreview {
        val current = directDefaults(organizationId, userId)
        val before = effectiveRoles.defaultRoleIds(organizationId, userId)
        val after = (before - (current - target)) + (target - current)
        val entryChanges = mutableListOf<EntryChange>()
        val roleChanges = linkedMapOf<String, List<String>>()
        val conflicts = mutableListOf<String>()
        for (tenant in organizationTenants(organizationId)) {
            val tenantOverrides = overrides.searchByTenantAndUser(tenant.id, userId)
            val assignedBefore = effectiveRoles.assignedRoleIds(organizationId, tenant.id, userId, before, tenantOverrides)
            val assignedAfter = effectiveRoles.assignedRoleIds(organizationId, tenant.id, userId, after, tenantOverrides)
            val effectiveBefore = effectiveRoles.effectiveRoleIds(organizationId, tenant.id, userId, assigned = assignedBefore)
            val effectiveAfter = effectiveRoles.effectiveRoleIds(organizationId, tenant.id, userId, assigned = assignedAfter)
            if (effectiveBefore.isEmpty() && effectiveAfter.isNotEmpty()) entryChanges += EntryChange(tenant.id, tenant.name, regains = true)
            if (effectiveBefore.isNotEmpty() && effectiveAfter.isEmpty()) entryChanges += EntryChange(tenant.id, tenant.name, regains = false)
            val changed = ((effectiveAfter - effectiveBefore).map { "+$it" } + (effectiveBefore - effectiveAfter).map { "-$it" })
            if (changed.isNotEmpty()) roleChanges[tenant.id] = changed
            sodConflict(organizationId, effectiveAfter)?.let { conflicts += "${tenant.id}:$it" }
        }
        return DefaultRolesPreview(
            revision = recorder.current(organizationId),
            addedRoleIds = (target - current).sorted(),
            removedRoleIds = (current - target).sorted(),
            entryChanges = entryChanges,
            tenantRoleChanges = roleChanges,
            sodConflicts = conflicts,
        )
    }

    /** The member's live direct grants on this organization's roles. */
    private fun directDefaults(organizationId: String, userId: String): Set<String> =
        effectiveRoles.organizationRoles(organizationId, roleUsers.searchRoleIdsByUserId(userId)).keys

    private fun assertNoSodConflict(organizationId: String, userId: String, tenantIds: Collection<String>) =
        sod.assertNoConflict(organizationId, userId, tenantIds)

    private fun sodConflict(organizationId: String, roleIds: Set<String>): String? = sod.conflict(organizationId, roleIds)

    private fun requireOrganizationRoles(organizationId: String, roleIds: Collection<String>): Set<String> {
        val found = effectiveRoles.organizationRoles(organizationId, roleIds)
        require(found.keys == roleIds.toSet()) { "ORGANIZATION_ROLE_NOT_FOUND" }
        return found.keys
    }

    private fun requireMember(organizationId: String, userId: String) =
        requireNotNull(accounts.get(userId)?.takeIf { it.organizationId == organizationId }) { "ORGANIZATION_MEMBER_NOT_FOUND" }

    private fun organizationTenants(organizationId: String): List<SysTenant> =
        tenants.search(Criteria(SysTenant::organizationId eq organizationId)).sortedBy { it.name }

    private fun catalog(organizationId: String): List<OrganizationRoleRow> =
        roles.search(Criteria(AuthRole::tenantId eq organizationId))
            .map { OrganizationRoleRow(it.id, it.code, it.name, it.subsysCode, it.active) }
            .sortedWith(compareBy({ it.subSystemCode }, { it.code }))
}
