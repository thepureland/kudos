package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.sys.core.organization.OrganizationMode
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Whatever path changes an organization member's defaults — a direct role bind, a temporal grant, an
 * approved request, a group membership or a group's roles — the change is checked against duty
 * separation in every tenant and recorded on the organization's revision, inside the changing
 * transaction (these events are published synchronously by the writers, before commit). A conflict
 * rolls the change back.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationGrantChangeListener(
    private val mode: OrganizationMode,
    private val roles: AuthRoleDao,
    private val groups: AuthGroupDao,
    private val groupUsers: AuthGroupUserDao,
    private val sod: OrganizationSodChecker,
    private val recorder: OrganizationChangeRecorder,
) {

    @EventListener
    open fun on(event: AuthRoleUserRelationsChanged) {
        val organizationId = roles.get(event.roleId)?.organizationId ?: return
        changed(organizationId, event.userIds, "DEFAULT_GRANT_CHANGED", event.roleId)
    }

    @EventListener
    open fun on(event: AuthGroupUserRelationsChanged) {
        val organizationId = groups.get(event.groupId)?.organizationId ?: return
        changed(organizationId, event.userIds, "GROUP_MEMBERSHIP_CHANGED", event.groupId)
    }

    @EventListener
    open fun on(event: AuthGroupRoleRelationsChanged) {
        val organizationId = groups.get(event.groupId)?.organizationId ?: return
        changed(organizationId, groupUsers.searchUserIdsByGroupId(event.groupId), "GROUP_ROLES_CHANGED", event.groupId)
    }

    private fun changed(organizationId: String, userIds: Collection<String>, action: String, targetId: String) {
        if (!mode.enabled || userIds.isEmpty()) return
        recorder.begin(organizationId)
        userIds.distinct().forEach { sod.assertNoConflict(organizationId, it) }
        recorder.commit(organizationId, action, targetId = targetId, detail = "members=${userIds.distinct().size}")
    }
}
