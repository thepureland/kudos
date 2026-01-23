package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.role.AuthRoleRecord
import io.kudos.ams.auth.common.vo.role.AuthRoleSearchPayload
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.service.iservice.IAuthRoleService
import io.kudos.ams.auth.provider.model.po.AuthRole
import io.kudos.ams.auth.provider.dao.AuthRoleDao
import io.kudos.ams.auth.provider.cache.RoleByIdCacheHandler
import io.kudos.ams.auth.provider.cache.RoleIdByTenantIdAndRoleCodeCacheHandler
import io.kudos.ams.auth.provider.cache.UserByIdCacheHandler
import io.kudos.ams.auth.provider.cache.UserIdsByRoleIdCacheHandler
import io.kudos.ams.auth.provider.cache.ResourceIdsByRoleIdCacheHandler
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.sys.provider.cache.ResourceByIdCacheHandler
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
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
open class AuthRoleService : BaseCrudService<String, AuthRole, AuthRoleDao>(), IAuthRoleService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userIdsByRoleIdCacheHandler: UserIdsByRoleIdCacheHandler

    @Autowired
    private lateinit var resourceIdsByRoleIdCacheHandler: ResourceIdsByRoleIdCacheHandler

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    @Autowired
    private lateinit var roleByIdCacheHandler: RoleByIdCacheHandler

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getRoleUserIds(roleId: String): List<String> {
        return userIdsByRoleIdCacheHandler.getUserIds(roleId)
    }

    override fun getRoleResourceIds(roleId: String): List<String> {
        return resourceIdsByRoleIdCacheHandler.getResourceIds(roleId)
    }

    override fun getRoleIds(tenantId: String): List<String> {
        val criteria = Criteria(AuthRole::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(AuthRole::active.name, OperatorEnum.EQ, true)
        val roles = search(criteria)
        return roles.mapNotNull { it.id }
    }

    override fun getRoleUsers(roleId: String): List<AuthUserCacheItem> {
        val userIds = getRoleUserIds(roleId)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val usersMap = userByIdCacheHandler.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    override fun getRoleResources(roleId: String): List<SysResourceCacheItem> {
        val resourceIds = getRoleResourceIds(roleId)
        if (resourceIds.isEmpty()) {
            return emptyList()
        }
        val resourcesMap = resourceByIdCacheHandler.getResourcesByIds(resourceIds)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    override fun hasResource(roleId: String, resourceId: String): Boolean {
        val resourceIds = getRoleResourceIds(roleId)
        return resourceIds.contains(resourceId)
    }

    override fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheItem? {
        val roleId = roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, roleCode)
        return roleId?.let { roleByIdCacheHandler.getRoleById(it) }
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
        return dao.search(searchPayload) as List<AuthRoleRecord>
    }

    override fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRecord> {
        val searchPayload = AuthRoleSearchPayload().apply {
            this.tenantId = tenantId
            this.subsysCode = subsysCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<AuthRoleRecord>
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val role = AuthRole {
            this.id = id
            this.active = active
        }
        val success = dao.update(role)
        if (success) {
            log.debug("更新id为${id}的角色的启用状态为${active}。")
            roleByIdCacheHandler.syncOnUpdate(id)
            val existingRole = dao.get(id)
            if (existingRole != null) {
                roleIdByTenantIdAndRoleCodeCacheHandler.syncOnUpdateActive(id, active)
            }
        } else {
            log.error("更新id为${id}的角色的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的角色。")
        roleByIdCacheHandler.syncOnInsert(id)
        roleIdByTenantIdAndRoleCodeCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthRole::id.name) as String
        if (success) {
            log.debug("更新id为${id}的角色。")
            roleByIdCacheHandler.syncOnUpdate(id)
            roleIdByTenantIdAndRoleCodeCacheHandler.syncOnUpdate(any, id)
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
            roleByIdCacheHandler.syncOnDelete(id)
            roleIdByTenantIdAndRoleCodeCacheHandler.syncOnDelete(role, id)
        } else {
            log.error("删除id为${id}的角色失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val roles = dao.inSearchById(ids)
        val tenantAndCodes = roles.mapNotNull { role ->
            role.id?.let { Pair(role.tenantId, role.code) }
        }
        val count = super.batchDelete(ids)
        log.debug("批量删除角色，期望删除${ids.size}条，实际删除${count}条。")
        roleByIdCacheHandler.syncOnBatchDelete(ids)
        roleIdByTenantIdAndRoleCodeCacheHandler.syncOnBatchDelete(ids, tenantAndCodes)
        return count
    }

    //endregion your codes 2

}
