package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.common.role.vo.request.AuthRoleQuery
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByRoleIdCache
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleInserted
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Role business
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleService(
    dao: AuthRoleDao
) : BaseCrudService<String, AuthRole, AuthRoleDao>(dao),
    IAuthRoleService {


    @Resource
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Resource
    private lateinit var resourceIdsByRoleIdCache: ResourceIdsByRoleIdCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var sysResourceHashCache: SysResourceHashCache

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    @Resource
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getRoleUserIds(roleId: String): List<String> = userIdsByRoleIdCache.getUserIds(roleId)

    @Transactional(readOnly = true)
    override fun getRoleResourceIds(roleId: String): Set<String> =
        resourceIdsByRoleIdCache.getResourceIds(roleId).toSet()

    @Transactional(readOnly = true)
    override fun getRoleIds(tenantId: String): List<String> = dao.searchActiveRoleIdsByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> {
        val userIds = getRoleUserIds(roleId)
        if (userIds.isEmpty()) return emptyList()
        val usersMap = userAccountHashCache.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> {
        val resourceIds = getRoleResourceIds(roleId)
        if (resourceIds.isEmpty()) return emptyList()
        val resourcesMap = sysResourceHashCache.getResourcesByIds(resourceIds)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    @Transactional(readOnly = true)
    override fun hasResource(roleId: String, resourceId: String): Boolean =
        resourceId in getRoleResourceIds(roleId)

    @Transactional(readOnly = true)
    override fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheEntry? =
        authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
            ?.let { authRoleHashCache.getRoleById(it) }

    @Transactional(readOnly = true)
    override fun getRoleRecord(id: String): AuthRoleRow? = dao.getAs<AuthRoleRow>(id)

    @Transactional(readOnly = true)
    override fun getRolesByTenantId(tenantId: String): List<AuthRoleRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(AuthRoleQuery(tenantId = tenantId), AuthRoleRow::class)

    @Transactional(readOnly = true)
    override fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(AuthRoleQuery(tenantId = tenantId, subsysCode = subsysCode), AuthRoleRow::class)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val role = AuthRole.Companion {
            this.id = id
            this.active = active
        }
        val success = dao.update(role)
        if (success) {
            log.debug("Updated active status of role with id ${id} to ${active}.")
            eventPublisher.publishEvent(AuthRoleUpdated(id))
        } else {
            log.error("Failed to update active status of role with id ${id} to ${active}!")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("Added role with id ${id}.")
        eventPublisher.publishEvent(AuthRoleInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthRole::id.name) as String
        if (success) {
            log.debug("Updated role with id ${id}.")
            eventPublisher.publishEvent(AuthRoleUpdated(id))
        } else {
            log.error("Failed to update role with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val role = dao.get(id)
        if (role == null) {
            log.warn("Role with id ${id} no longer exists when attempting to delete!")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("Deleted role with id ${id}.")
            eventPublisher.publishEvent(AuthRoleDeleted(id, role.tenantId, role.code))
        } else {
            log.error("Failed to delete role with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // Snapshot tenantId/code first; after AFTER_COMMIT the rows are deleted and downstream (tenantId, code) caches
        // can no longer query back.
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map { AuthRoleBatchDeleted.Item(it.id, it.tenantId, it.code) }
        val count = super.batchDelete(ids)
        log.debug("Batch delete roles: expected to delete ${ids.size} entries, actually deleted ${count}.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(AuthRoleBatchDeleted(snapshots))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun hasRole(userId: String, roleId: String): Boolean = roleId in getUserRoleIds(userId)

    @Transactional(readOnly = true)
    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean =
        authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
            ?.let { hasRole(userId, it) } == true

    @Transactional(readOnly = true)
    override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> {
        val roleIds = getUserRoleIds(userId)
        if (roleIds.isEmpty()) return emptyList()
        val rolesMap = authRoleHashCache.getRolesByIds(roleIds)
        return roleIds.mapNotNull { rolesMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getUserRoleIds(userId: String): List<String> = roleIdsByUserIdCache.getRoleIds(userId)

    @Transactional(readOnly = true)
    override fun getUsersByRoleCode(tenantId: String, roleCode: String): List<UserAccountCacheEntry> {
        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id ?: return emptyList()
        val userIds = userIdsByRoleIdCache.getUserIds(roleId)
        if (userIds.isEmpty()) return emptyList()
        return userAccountHashCache.getUsersByIds(userIds).values.toList()
    }

    @Transactional(readOnly = true)
    override fun isUserHasResource(userId: String, resourceId: String): Boolean =
        resourceId in getUserResourceIds(userId)

    @Transactional(readOnly = true)
    override fun getUserResourceIds(userId: String): Set<String> =
        resourceIdsByUserIdCache.getResourceIds(userId).toSet()

    @Transactional(readOnly = true)
    override fun getResources(userId: String): List<SysResourceCacheEntry> {
        // Get the list of resource IDs by user ID
        val resourceIds = resourceIdsByUserIdCache.getResourceIds(userId)
        if (resourceIds.isEmpty()) return emptyList()
        // Batch fetch resource cache objects (sysResourceHashCache.getResourcesByIds accepts a Set, perform a dedup conversion)
        val resourcesMap = sysResourceHashCache.getResourcesByIds(resourceIds.toSet())
        // Return resource list (in original ID order)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }


}
