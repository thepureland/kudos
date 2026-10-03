package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.core.organization.dao.AuthManagementRoleGrantDao
import io.kudos.ms.auth.core.organization.dao.AuthOrganizationAdminDao
import io.kudos.ms.auth.core.organization.dao.AuthTenantRoleOverrideDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * A deleted organization account takes its overrides, administrator designation and management roles
 * with it (in the deleting transaction, so nothing dangles if it commits). A disabled account keeps
 * them, but none of them count while it is inactive.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationMembershipCleanup(
    private val overrides: AuthTenantRoleOverrideDao,
    private val admins: AuthOrganizationAdminDao,
    private val managementRoles: AuthManagementRoleGrantDao,
    private val recorder: OrganizationChangeRecorder,
) {

    @EventListener
    open fun on(event: UserAccountDeleted) = cleanup(event.id, event.organizationId)

    @EventListener
    open fun on(event: UserAccountBatchDeleted) = event.items.forEach { cleanup(it.id, it.organizationId) }

    private fun cleanup(userId: String, organizationId: String?) {
        if (organizationId.isNullOrBlank()) return
        recorder.begin(organizationId)
        val removed = overrides.deleteByUser(userId) + admins.deleteByUser(userId) + managementRoles.deleteByUser(userId)
        if (removed > 0) recorder.commit(organizationId, "MEMBERSHIP_ENDED", targetUserId = userId, detail = "removedRows=$removed")
    }
}
