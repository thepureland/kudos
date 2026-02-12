package io.kudos.ms.auth.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ms.auth.common.vo.role.AuthRoleRecord
import io.kudos.ms.auth.common.vo.role.AuthRoleSearchPayload
import io.kudos.ms.auth.core.cache.*
import io.kudos.ms.auth.core.dao.AuthRoleDao
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.ms.auth.core.service.iservice.IAuthRoleService
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.core.cache.SysResourceHashCache
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.cache.UserAccountHashCache
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 角色业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleService : BaseCrudService<String, AuthRole, AuthRoleDao>(),
    IAuthRoleService {
//endregion your codes 1

    //region your codes 2

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

    private val log = LogFactory.getLog(this)

    override fun getRoleUserIds(roleId: String): List<String> {
        return userIdsByRoleIdCache.getUserIds(roleId)
    }

    override fun getRoleResourceIds(roleId: String): Set<String> {
        return resourceIdsByRoleIdCache.getResourceIds(roleId)
    }

    override fun getRoleIds(tenantId: String): List<String> {
        return dao.searchActiveRoleIdsByTenantId(tenantId)
    }

    override fun getRoleUsers(roleId: String): List<UserAccountCacheItem> {
        val userIds = getRoleUserIds(roleId)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val usersMap = userAccountHashCache.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    override fun getRoleResources(roleId: String): List<SysResourceCacheItem> {
        val resourceIds = getRoleResourceIds(roleId)
        if (resourceIds.isEmpty()) {
            return emptyList()
        }
        val resourcesMap = sysResourceHashCache.getResourcesByIds(resourceIds)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    override fun hasResource(roleId: String, resourceId: String): Boolean {
        val resourceIds = getRoleResourceIds(roleId)
        return resourceIds.contains(resourceId)
    }

    override fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheItem? {
        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
        return roleId?.let { authRoleHashCache.getRoleById(it) }
    }

    override fun getRoleRecord(id: String): AuthRoleRecord? {
        val role = dao.get(id) ?: return null
        val record = AuthRoleRecord()
        BeanKit.copyProperties(role, record)
        return record
    }

    override fun getRolesByTenantId(tenantId: String): List<AuthRoleRecord> {
        val searchPayload = AuthRoleSearchPayload().apply {
            this.tenantId = tenantId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, AuthRoleRecord::class)
    }

    override fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRecord> {
        val searchPayload = AuthRoleSearchPayload().apply {
            this.tenantId = tenantId
            this.subsysCode = subsysCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, AuthRoleRecord::class)
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val role = AuthRole.Companion {
            this.id = id
            this.active = active
        }
        val success = dao.update(role)
        if (success) {
            log.debug("更新id为${id}的角色的启用状态为${active}。")
            authRoleHashCache.syncOnUpdate(id)
//            val existingRole = dao.get(id)
//            if (existingRole != null) {
//                roleIdByTenantIdAndRoleCodeCache.syncOnUpdateActive(id, active)
//            }
        } else {
            log.error("更新id为${id}的角色的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的角色。")
        authRoleHashCache.syncOnInsert(id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthRole::id.name) as String
        if (success) {
            log.debug("更新id为${id}的角色。")
            authRoleHashCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的角色失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val role = dao.get(id)
        if (role == null) {
            log.warn("删除id为${id}的角色时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的角色。")
            authRoleHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的角色失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除角色，期望删除${ids.size}条，实际删除${count}条。")
        authRoleHashCache.syncOnBatchDelete(ids)
        return count
    }

    override fun hasRole(userId: String, roleId: String): Boolean {
        val roleIds = getUserRoleIds(userId)
        return roleIds.contains(roleId)
    }

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean {
        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
        return roleId != null && hasRole(userId, roleId)
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheItem> {
        val roleIds = getUserRoleIds(userId)
        if (roleIds.isEmpty()) {
            return emptyList()
        }
        val rolesMap = authRoleHashCache.getRolesByIds(roleIds)
        return roleIds.mapNotNull { rolesMap[it] }
    }

    override fun getUserRoleIds(userId: String): List<String> {
        return roleIdsByUserIdCache.getRoleIds(userId)
    }

    override fun getUsersByRoleCode(tenantId: String, roleCode: String): List<UserAccountCacheItem> {
        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id ?: return emptyList()
        val userIds = userIdsByRoleIdCache.getUserIds(roleId)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        return userAccountHashCache.getUsersByIds(userIds).values.toList()
    }

    override fun isUserHasResource(userId: String, resourceId: String): Boolean {
        val resourceIds = getUserResourceIds(userId)
        return resourceIds.contains(resourceId)
    }

    override fun getUserResourceIds(userId: String): Set<String> {
        return resourceIdsByUserIdCache.getResourceIds(userId)
    }

    override fun getResources(userId: String): List<SysResourceCacheItem> {
        // 通过用户ID获取资源ID列表
        val resourceIds = resourceIdsByUserIdCache.getResourceIds(userId)

        // 如果没有资源，返回空列表
        if (resourceIds.isEmpty()) {
            return emptyList()
        }

        // 批量获取资源缓存对象
        val resourcesMap = sysResourceHashCache.getResourcesByIds(resourceIds)

        // 返回资源列表（按原始ID顺序）
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    //endregion your codes 2

}
