package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.provider.cache.DeptByIdCacheHandler
import io.kudos.ams.auth.provider.cache.ResourceIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.cache.RoleByIdCacheHandler
import io.kudos.ams.auth.provider.cache.RoleIdByTenantIdAndRoleCodeCacheHandler
import io.kudos.ams.auth.provider.cache.RoleIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.cache.DeptIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.ams.auth.provider.service.iservice.IAuthUserService
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.sys.provider.cache.ResourceByIdCacheHandler
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 用户业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthUserService : BaseCrudService<String, AuthUser, AuthUserDao>(), IAuthUserService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var resourceIdsByUserIdCacheHandler: ResourceIdsByUserIdCacheHandler

    @Autowired
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    @Autowired
    private lateinit var roleIdsByUserIdCacheHandler: RoleIdsByUserIdCacheHandler

    @Autowired
    private lateinit var deptIdsByUserIdCacheHandler: DeptIdsByUserIdCacheHandler

    @Autowired
    private lateinit var roleByIdCacheHandler: RoleByIdCacheHandler

    @Autowired
    private lateinit var deptByIdCacheHandler: DeptByIdCacheHandler

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    override fun getResources(userId: String): List<SysResourceCacheItem> {
        // 通过用户ID获取资源ID列表
        val resourceIds = resourceIdsByUserIdCacheHandler.getResourceIds(userId)
        
        // 如果没有资源，返回空列表
        if (resourceIds.isEmpty()) {
            return emptyList()
        }
        
        // 批量获取资源缓存对象
        val resourcesMap = resourceByIdCacheHandler.getResourcesByIds(resourceIds)
        
        // 返回资源列表（按原始ID顺序）
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    override fun getUserRoleIds(userId: String): List<String> {
        return roleIdsByUserIdCacheHandler.getRoleIds(userId)
    }

    override fun getUserDeptIds(userId: String): List<String> {
        return deptIdsByUserIdCacheHandler.getDeptIds(userId)
    }

    override fun getUserResourceIds(userId: String): List<String> {
        return resourceIdsByUserIdCacheHandler.getResourceIds(userId)
    }

    override fun getUserIds(tenantId: String): List<String> {
        val criteria = Criteria(AuthUser::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(AuthUser::active.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val users = search(criteria) as List<AuthUser>
        return users.mapNotNull { it.id }
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheItem> {
        val roleIds = getUserRoleIds(userId)
        if (roleIds.isEmpty()) {
            return emptyList()
        }
        val rolesMap = roleByIdCacheHandler.getRolesByIds(roleIds)
        return roleIds.mapNotNull { rolesMap[it] }
    }

    override fun getUserDepts(userId: String): List<AuthDeptCacheItem> {
        val deptIds = getUserDeptIds(userId)
        if (deptIds.isEmpty()) {
            return emptyList()
        }
        val deptsMap = deptByIdCacheHandler.getDeptsByIds(deptIds)
        return deptIds.mapNotNull { deptsMap[it] }
    }

    override fun hasRole(userId: String, roleId: String): Boolean {
        val roleIds = getUserRoleIds(userId)
        return roleIds.contains(roleId)
    }

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean {
        val roleId = roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, roleCode)
        return roleId != null && hasRole(userId, roleId)
    }

    override fun isUserInDept(userId: String, deptId: String): Boolean {
        val deptIds = getUserDeptIds(userId)
        return deptIds.contains(deptId)
    }

    override fun hasResource(userId: String, resourceId: String): Boolean {
        val resourceIds = getUserResourceIds(userId)
        return resourceIds.contains(resourceId)
    }

    //endregion your codes 2

}
