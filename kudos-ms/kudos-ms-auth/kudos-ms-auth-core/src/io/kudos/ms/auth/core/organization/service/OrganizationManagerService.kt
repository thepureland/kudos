package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.core.organization.ManagementRoleKind
import io.kudos.ms.auth.core.organization.dao.AuthManagementRoleGrantDao
import io.kudos.ms.auth.core.organization.dao.AuthOrganizationAdminDao
import io.kudos.ms.auth.core.organization.model.po.AuthManagementRoleGrant
import io.kudos.ms.auth.core.organization.model.po.AuthOrganizationAdmin
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.dao.UserAccountDao
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** An organization administrator, as listed. */
data class OrganizationAdminRow(val userId: String, val username: String?, val active: Boolean, val assignedBy: String?, val assignedTime: LocalDateTime?)

/** A management role grant, as listed. */
data class ManagementRoleRow(val userId: String, val username: String?, val roleKind: String, val tenantId: String?, val grantedBy: String?)

/**
 * Organization administrator designations changed. Published in the changing transaction so that a
 * notification listener can tell the platform and the other administrators (G-10).
 */
data class OrganizationAdministratorChanged(
    val organizationId: String,
    val userId: String,
    val assigned: Boolean,
    val actorId: String?,
    val remainingAdminIds: List<String>,
)

/**
 * Organization administrators (G-6, G-10, G-16) and built-in management roles (G-7, G-11).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class OrganizationManagerService(
    private val admins: AuthOrganizationAdminDao,
    private val managementRoles: AuthManagementRoleGrantDao,
    private val accounts: UserAccountDao,
    private val tenants: SysTenantDao,
    private val authority: OrganizationAuthority,
    private val policy: OrganizationAdministrationPolicy,
    private val recorder: OrganizationChangeRecorder,
    private val events: ApplicationEventPublisher,
) {

    @Transactional(readOnly = true)
    open fun listAdmins(organizationId: String): List<OrganizationAdminRow> {
        policy.assertCanReadOrganization(organizationId)
        return admins.searchByOrganization(organizationId).map { row ->
            val account = accounts.get(row.userId)
            OrganizationAdminRow(row.userId, account?.username, account?.active == true, row.createUserId, row.createTime)
        }
    }

    open fun assignAdmin(organizationId: String, userId: String, reason: String?): Long {
        policy.assertCanDesignateAdmin(organizationId, userId)
        recorder.begin(organizationId)
        if (admins.find(organizationId, userId) != null) return recorder.current(organizationId)
        admins.insert(AuthOrganizationAdmin {
            this.organizationId = organizationId
            this.userId = userId
            this.createUserId = CurrentUserKit.currentUserIdOrNull()
            this.createTime = LocalDateTime.now()
        })
        val revision = recorder.commit(organizationId, "ORGANIZATION_ADMIN_ASSIGNED", targetUserId = userId, reason = reason)
        notifyAdmins(organizationId, userId, assigned = true)
        return revision
    }

    open fun revokeAdmin(organizationId: String, userId: String, reason: String?): Long {
        policy.assertCanDesignateAdmin(organizationId, userId)
        recorder.begin(organizationId)
        val row = admins.find(organizationId, userId) ?: return recorder.current(organizationId)
        // Checked under the organization lock: two administrators revoking each other cannot both win.
        require(authority.organizationAdminIds(organizationId).any { it != userId }) { "ORGANIZATION_LAST_ADMIN" }
        admins.deleteById(row.id)
        val revision = recorder.commit(organizationId, "ORGANIZATION_ADMIN_REVOKED", targetUserId = userId, reason = reason)
        notifyAdmins(organizationId, userId, assigned = false)
        return revision
    }

    @Transactional(readOnly = true)
    open fun listManagementRoles(organizationId: String): List<ManagementRoleRow> {
        policy.assertCanReadOrganization(organizationId)
        return managementRoles.searchByOrganization(organizationId).map { row ->
            ManagementRoleRow(row.userId, accounts.get(row.userId)?.username, row.roleKind, row.tenantId.takeIf(String::isNotBlank), row.createUserId)
        }
    }

    /** Grants [kind]; a tenant permission administrator needs the [tenantIds] it governs. */
    open fun grantManagementRole(
        organizationId: String, userId: String, kind: ManagementRoleKind, tenantIds: Collection<String>, reason: String?,
    ): Long {
        policy.assertCanGrantManagementRole(organizationId, userId, kind)
        val scopes = scopesOf(organizationId, kind, tenantIds)
        recorder.begin(organizationId)
        val existing = managementRoles.searchByUser(organizationId, userId).filter { it.roleKind == kind.name }.map { it.tenantId }.toSet()
        val added = scopes - existing
        if (added.isEmpty()) return recorder.current(organizationId)
        added.forEach { tenantId ->
            managementRoles.insert(AuthManagementRoleGrant {
                this.organizationId = organizationId
                this.userId = userId
                this.roleKind = kind.name
                this.tenantId = tenantId
                this.createUserId = CurrentUserKit.currentUserIdOrNull()
                this.createTime = LocalDateTime.now()
            })
        }
        return recorder.commit(organizationId, "MANAGEMENT_ROLE_GRANTED", targetUserId = userId, targetId = kind.name,
            detail = added.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let { "tenants=$it" }, reason = reason)
    }

    /** Revokes [kind]; for a tenant permission administrator only the given [tenantIds], or all when empty. */
    open fun revokeManagementRole(
        organizationId: String, userId: String, kind: ManagementRoleKind, tenantIds: Collection<String>, reason: String?,
    ): Long {
        // Revoking takes the same rank as granting (G-11).
        policy.assertCanGrantManagementRole(organizationId, userId, kind)
        recorder.begin(organizationId)
        val rows = managementRoles.searchByUser(organizationId, userId)
            .filter { it.roleKind == kind.name && (tenantIds.isEmpty() || it.tenantId in tenantIds) }
        if (rows.isEmpty()) return recorder.current(organizationId)
        rows.forEach { managementRoles.deleteById(it.id) }
        return recorder.commit(organizationId, "MANAGEMENT_ROLE_REVOKED", targetUserId = userId, targetId = kind.name,
            detail = rows.map { it.tenantId }.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let { "tenants=$it" }, reason = reason)
    }

    private fun scopesOf(organizationId: String, kind: ManagementRoleKind, tenantIds: Collection<String>): Set<String> =
        when (kind) {
            ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN -> setOf("")
            ManagementRoleKind.TENANT_PERMISSION_ADMIN -> {
                require(tenantIds.isNotEmpty()) { "ORGANIZATION_TENANT_REQUIRED" }
                tenantIds.forEach { require(tenants.get(it)?.organizationId == organizationId) { "ORGANIZATION_TENANT_MISMATCH" } }
                tenantIds.toSet()
            }
        }

    private fun notifyAdmins(organizationId: String, userId: String, assigned: Boolean) =
        events.publishEvent(OrganizationAdministratorChanged(
            organizationId, userId, assigned, CurrentUserKit.currentUserIdOrNull(), authority.organizationAdminIds(organizationId),
        ))
}
