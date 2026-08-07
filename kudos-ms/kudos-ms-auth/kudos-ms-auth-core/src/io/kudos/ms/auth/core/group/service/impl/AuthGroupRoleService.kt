package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.kudos.ms.user.common.passport.CurrentUserKit


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

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var tenantAdministrationGuard: TenantAdministrationGuard

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getRoleIdsByGroupId(groupId: String): Set<String> {
        val group = authGroupDao.get(groupId) ?: throw IllegalArgumentException("Group not found: $groupId")
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        return dao.searchRoleIdsByGroupId(groupId)
    }

    @Transactional(readOnly = true)
    override fun getGroupIdsByRoleId(roleId: String): Set<String> {
        val role = authRoleDao.get(roleId) ?: throw IllegalArgumentException("Role not found: $roleId")
        tenantAdministrationGuard.assertCanManage(role.tenantId)
        return dao.searchGroupIdsByRoleId(roleId)
    }

    @Transactional
    override fun batchBind(groupId: String, roleIds: Collection<String>): Int {
        if (roleIds.isEmpty()) return 0
        val group = authGroupDao.get(groupId) ?: throw IllegalArgumentException("Group not found: $groupId")
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        val roles = authRoleDao.getByIds(roleIds.toSet()).associateBy { it.id }
        roleIds.toSet().forEach { roleId ->
            val role = roles[roleId] ?: throw IllegalArgumentException("Role not found: $roleId")
            require(role.active) { "Role $roleId is inactive." }
            require(role.tenantId == group.tenantId) {
                "Group $groupId and role $roleId belong to different tenants."
            }
            require(role.subsysCode == group.subsysCode) {
                "Group $groupId and role $roleId belong to different subsystems."
            }
        }
        val rows = dao.searchBindingsByGroupId(groupId)
        val requested = roleIds.toSet()
        val revivable = rows.filter { it.revoked == true && it.roleId in requested }
        val brandNew = requested - rows.map { it.roleId }.toSet()
        val newRoleIds = brandNew + revivable.map { it.roleId }
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

        val operatorId = CurrentUserKit.currentUserIdOrNull()
        val relations = brandNew.map { roleId ->
            AuthGroupRole {
                this.groupId = groupId
                this.roleId = roleId
                this.grantedBy = operatorId
                this.revoked = false
            }
        }
        if (relations.isNotEmpty()) dao.batchInsert(relations)
        revivable.forEach { row ->
            dao.update(AuthGroupRole {
                this.id = row.id
                this.grantedBy = operatorId
                this.revoked = false
                this.revokeReason = null
            })
        }
        log.debug(
            "Batch-bound group $groupId: ${brandNew.size} new role bindings, " +
                "${revivable.size} revoked bindings reinstated.",
        )
        eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, newRoleIds.toList()))
        return newRoleIds.size
    }

    @Transactional
    override fun unbind(groupId: String, roleId: String): Boolean {
        val group = authGroupDao.get(groupId) ?: throw IllegalArgumentException("Group not found: $groupId")
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        val row = dao.searchBindingsByGroupId(groupId)
            .firstOrNull { it.roleId == roleId && it.revoked != true }
        val success = row != null
        if (success) {
            dao.update(AuthGroupRole {
                this.id = row.id
                this.revoked = true
                this.revokeReason = "unbound from group $groupId"
            })
            log.debug("Unbound group ${groupId} from role ${roleId}.")
            eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, listOf(roleId)))
        } else {
            log.warn("Failed to unbind group ${groupId} from role ${roleId}: relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(groupId: String, roleId: String): Boolean {
        val group = authGroupDao.get(groupId) ?: return false
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        return dao.searchRoleIdsByGroupId(groupId).contains(roleId)
    }


}
