package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Group-role relation service.
 *
 * [batchBind] is the broadcast grant: one row hands the role to every current member of the group.
 * That makes it the widest-reaching write in the module and the one that most needs the shared
 * admission gate — see [IAuthGrantPolicyService].
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
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

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

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
        // The broadcast write of the whole module: this single row hands `newRoleIds` to every
        // current member at once. It is screened per (member, role) — and because the binding is one
        // row, it cannot be applied to some members only: any rejection aborts the call, naming the
        // members that blocked it so the admin can act on them.
        // Screened against the raw roster rather than the currently-in-force one: a future-dated
        // membership will receive these roles the moment its window opens, and by then there is no
        // second chance to refuse. Screening early can only be over-strict, never permissive.
        val memberIds = authGroupUserDao.searchMemberUserIdsByGroupId(groupId)
        if (memberIds.isNotEmpty()) {
            grantPolicyService.assertNoRejection(
                grantPolicyService.screenGrants(
                    newRoleIds.flatMap { roleId ->
                        memberIds.map { userId ->
                            GrantCandidate(roleId, userId, via = GrantCandidate.Via.GROUP_ROLE_BINDING)
                        }
                    },
                ),
            )
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
