package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.group.vo.response.GroupDeleteImpactVo
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupInserted
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
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

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("Inserted user group with id ${id}.")
        eventPublisher.publishEvent(AuthGroupInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthGroup::id.name) as String
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
        val success = super.deleteById(id)
        if (success) {
            log.debug("Deleted user group with id ${id}.")
            eventPublisher.publishEvent(AuthGroupDeleted(id, group.tenantId, group.code))
        } else {
            log.warn("Failed to delete user group with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // Snapshot tenantId/code up front; downstream (tenantId, code) caches cannot look these up after AFTER_COMMIT.
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map { AuthGroupBatchDeleted.Item(it.id, it.tenantId, it.code) }
        val count = super.batchDelete(ids)
        log.debug("Batch-deleted user groups: expected ${ids.size}, actually deleted ${count}.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(AuthGroupBatchDeleted(snapshots))
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
