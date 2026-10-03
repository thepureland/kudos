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
import org.ktorm.entity.Entity
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.auth.common.group.vo.request.AuthGroupQuery
import io.kudos.ms.auth.core.policy.AdministrationOwner
import kotlin.reflect.KClass


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

    /**
     * Organization principals read their own organization's groups only; everybody else reads exactly
     * as before.
     */
    @Transactional(readOnly = true)
    override fun get(id: String): AuthGroup? {
        val organizationId = tenantAdministrationGuard.callerOrganization() ?: return super.get(id)
        return super.get(id)?.also { requireOwnRow(it.tenantId, organizationId) }
    }

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        val organizationId = tenantAdministrationGuard.callerOrganization() ?: return super.get(id, returnType)
        val row = dao.get(id) ?: return null
        requireOwnRow(row.tenantId, organizationId)
        return super.get(id, returnType)
    }

    private fun requireOwnRow(ownerId: String, organizationId: String) =
        require(ownerId == organizationId) { "cross-organization authorization administration is forbidden." }

    /**
     * Organization principals list their own organization's groups only (organization groups keep
     * the organization id as their owner); everybody else lists exactly as before.
     */
    @Transactional(readOnly = true)
    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val organizationId = tenantAdministrationGuard.callerOrganization() ?: return super.pagingSearch(listSearchPayload)
        require(listSearchPayload is AuthGroupQuery) { "An organization-scoped group query is required" }
        require(listSearchPayload.organizationId.isNullOrBlank() || listSearchPayload.organizationId == organizationId) {
            "cross-organization authorization administration is forbidden."
        }
        val scoped = listSearchPayload.copy(organizationId = organizationId, tenantId = organizationId).apply {
            pageNo = listSearchPayload.pageNo
            pageSize = listSearchPayload.pageSize
            orders = listSearchPayload.orders
        }
        return super.pagingSearch(scoped)
    }

    @Transactional
    override fun insert(any: Any): String {
        val requestedTenant = BeanKit.getProperty(any, AuthGroup::tenantId.name) as String?
        val requestedOrganization = BeanKit.getProperty(any, AuthGroup::organizationId.name) as String?
        val owner = if (tenantAdministrationGuard.requestsOrganizationOwner(requestedTenant, requestedOrganization)) {
            tenantAdministrationGuard.resolveOwner(requestedTenant, requestedOrganization)
        } else AdministrationOwner(requestedTenant, null)
        tenantAdministrationGuard.assertCanManage(requireNotNull(owner.ownerId) { "group tenantId is required." })
        // Organization groups carry their owner in both columns; legacy inserts go through untouched.
        val id = if (owner.organizationId == null) super.insert(any) else super.insert(
            Entity.create<AuthGroup>().also {
                BeanKit.copyProperties(any, it)
                it.tenantId = owner.organizationId
                it.organizationId = owner.organizationId
            },
        )
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
        // The generic update copies every form property, nulls included: an organization group keeps its owner columns.
        // Ownership is decided at creation: a tenant-owned row never becomes an organization row.
        val requestedOrganization = BeanKit.getProperty(any, AuthGroup::organizationId.name) as String?
        require(requestedOrganization.isNullOrBlank() || requestedOrganization == existing.organizationId) {
            "organizationId is immutable (stored=${existing.organizationId}, requested=$requestedOrganization)."
        }
        val success = if (existing.organizationId == null) super.update(any) else super.update(
            Entity.create<AuthGroup>().also {
                BeanKit.copyProperties(any, it)
                it.tenantId = existing.tenantId
                it.organizationId = existing.organizationId
            },
        )
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
