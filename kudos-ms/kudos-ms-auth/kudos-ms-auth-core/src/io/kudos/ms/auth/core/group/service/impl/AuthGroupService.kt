package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.group.vo.response.GroupDeleteImpactVo
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupInserted
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * User group service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupService(
    dao: AuthGroupDao
) : BaseCrudService<String, AuthGroup, AuthGroupDao>(dao), IAuthGroupService {


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Resource
    private lateinit var authGroupUserService: IAuthGroupUserService

    @Resource
    private lateinit var authGroupRoleService: IAuthGroupRoleService

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var tenantAdministrationGuard: TenantAdministrationGuard

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun insert(any: Any): String {
        val tenantId = BeanKit.getProperty(any, AuthGroup::tenantId.name) as String?
        tenantAdministrationGuard.assertCanManage(requireNotNull(tenantId) { "group tenantId is required." })
        val id = super.insert(any)
        log.debug("Inserted user group with id ${id}.")
        eventPublisher.publishEvent(AuthGroupInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = BeanKit.getProperty(any, AuthGroup::id.name) as String
        val existing = dao.get(id) ?: throw IllegalArgumentException("Group not found: $id")
        tenantAdministrationGuard.assertCanManage(existing.tenantId)
        require(!existing.builtIn) { "built-in group $id cannot be modified." }
        val requestedTenant = BeanKit.getProperty(any, AuthGroup::tenantId.name) as String?
        val requestedSubsys = BeanKit.getProperty(any, AuthGroup::subsysCode.name) as String?
        require(requestedTenant == null || requestedTenant == existing.tenantId) {
            "group tenantId is immutable (stored=${existing.tenantId}, requested=$requestedTenant)."
        }
        require(requestedSubsys == null || requestedSubsys == existing.subsysCode) {
            "group subsysCode is immutable (stored=${existing.subsysCode}, requested=$requestedSubsys)."
        }
        val success = super.update(any)
        if (success) {
            log.debug("Updated user group with id ${id}.")
            eventPublisher.publishEvent(AuthGroupUpdated(id))
        } else {
            log.error("Failed to update user group with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val group = dao.get(id) ?: return run {
            log.warn("Attempted to delete user group with id ${id}, but it no longer exists!")
            false
        }
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        val userIds = authGroupUserDao.searchMemberUserIdsByGroupId(id)
        val roleIds = authGroupRoleDao.searchRoleIdsByGroupId(id)
        val success = super.deleteById(id)
        if (success) {
            authGroupUserDao.deleteByGroupId(id)
            authGroupRoleDao.deleteByGroupId(id)
            log.debug("Deleted user group with id ${id}.")
            eventPublisher.publishEvent(AuthGroupDeleted(id, group.tenantId, group.code))
            if (userIds.isNotEmpty()) eventPublisher.publishEvent(AuthGroupUserRelationsChanged(id, userIds))
            if (roleIds.isNotEmpty()) eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(id, roleIds))
        } else {
            log.warn("Failed to delete user group with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        // Snapshot tenantId/code up front; downstream (tenantId, code) caches cannot look these up after AFTER_COMMIT.
        val groups = dao.getByIds(ids)
        groups.forEach { tenantAdministrationGuard.assertCanManage(it.tenantId) }
        val snapshots = groups.map { AuthGroupBatchDeleted.Item(it.id, it.tenantId, it.code) }
        val relationSnapshots = groups.associate { group ->
            group.id to Pair(
                authGroupUserDao.searchMemberUserIdsByGroupId(group.id),
                authGroupRoleDao.searchRoleIdsByGroupId(group.id),
            )
        }
        val count = super.batchDelete(ids)
        snapshots.forEach {
            authGroupUserDao.deleteByGroupId(it.id)
            authGroupRoleDao.deleteByGroupId(it.id)
        }
        log.debug("Batch-deleted user groups: expected ${ids.size}, actually deleted ${count}.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(AuthGroupBatchDeleted(snapshots))
            relationSnapshots.forEach { (groupId, relations) ->
                if (relations.first.isNotEmpty()) {
                    eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, relations.first))
                }
                if (relations.second.isNotEmpty()) {
                    eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, relations.second))
                }
            }
        }
        return count
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun getDeleteImpact(groupIds: Collection<String>): GroupDeleteImpactVo {
        if (groupIds.isEmpty()) return GroupDeleteImpactVo.zero()
        // Union over all groups: a user belonging to multiple groups in the batch counts once.
        val allUserIds = HashSet<String>()
        val allRoleIds = HashSet<String>()
        for (gid in groupIds) {
            allUserIds.addAll(authGroupUserService.getUserIdsByGroupId(gid))
            allRoleIds.addAll(authGroupRoleService.getRoleIdsByGroupId(gid))
        }
        return GroupDeleteImpactVo(users = allUserIds.size, roles = allRoleIds.size)
    }

    @Transactional
    override fun batchBindUsers(groupIds: Collection<String>, userIds: Collection<String>): BatchBindResultVo {
        if (groupIds.isEmpty() || userIds.isEmpty()) return BatchBindResultVo.empty()
        var ok = 0
        val failures = mutableListOf<BatchBindResultVo.BatchBindFailure>()
        // Per-group transaction boundary mirrors the role-side semantics; partial failure is
        // surfaced to the admin UI rather than masked by a blanket rollback.
        for (gid in groupIds) {
            try {
                authGroupUserService.batchBind(gid, userIds)
                ok++
            } catch (e: Exception) {
                log.warn("Batch-bind failed for group ${gid}: ${e.message}")
                failures += BatchBindResultVo.BatchBindFailure(ownerId = gid, reason = e.message ?: e.javaClass.simpleName)
            }
        }
        return BatchBindResultVo(ok = ok, failures = failures)
    }


}
