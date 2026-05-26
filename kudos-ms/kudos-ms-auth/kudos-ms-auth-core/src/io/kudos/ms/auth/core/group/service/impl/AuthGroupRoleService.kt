package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Group-role relation service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupRoleService(
    dao: AuthGroupRoleDao
) : BaseCrudService<String, AuthGroupRole, AuthGroupRoleDao>(dao),
    IAuthGroupRoleService {


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getRoleIdsByGroupId(groupId: String): Set<String> =
        dao.searchRoleIdsByGroupId(groupId)

    @Transactional(readOnly = true)
    override fun getGroupIdsByRoleId(roleId: String): Set<String> =
        dao.searchGroupIdsByRoleId(roleId)

    @Transactional
    override fun batchBind(groupId: String, roleIds: Collection<String>): Int {
        if (roleIds.isEmpty()) return 0
        // One SELECT for existing relations, then a single batchInsert for the delta — collapses the original N+1 into 2 SQL calls.
        val existing = dao.searchRoleIdsByGroupId(groupId)
        val newRoleIds = roleIds.toSet() - existing
        if (newRoleIds.isEmpty()) {
            log.debug("Batch-binding group ${groupId} to ${roleIds.size} roles: all already exist, nothing inserted.")
            return 0
        }
        val relations = newRoleIds.map { roleId ->
            AuthGroupRole {
                this.groupId = groupId
                this.roleId = roleId
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch-bound group ${groupId} to ${roleIds.size} roles, ${newRoleIds.size} new bindings inserted.")
        eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, newRoleIds.toList()))
        return newRoleIds.size
    }

    @Transactional
    override fun unbind(groupId: String, roleId: String): Boolean {
        val count = dao.deleteByGroupIdAndRoleId(groupId, roleId)
        val success = count > 0
        if (success) {
            log.debug("Unbound group ${groupId} from role ${roleId}.")
            eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, listOf(roleId)))
        } else {
            log.warn("Failed to unbind group ${groupId} from role ${roleId}: relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(groupId: String, roleId: String): Boolean = dao.exists(groupId, roleId)


}
