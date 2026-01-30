package io.kudos.ams.auth.provider.api

import io.kudos.ams.auth.common.api.IAuthRoleApi
import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.user.common.vo.user.UserAccountCacheItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 角色 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@Service
open class AuthRoleApi : IAuthRoleApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var roleByIdCacheHandler: io.kudos.ams.auth.provider.cache.RoleByIdCacheHandler

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: io.kudos.ams.auth.provider.cache.RoleIdByTenantIdAndRoleCodeCacheHandler

    @Autowired
    private lateinit var authRoleService: io.kudos.ams.auth.provider.service.iservice.IAuthRoleService

    @Autowired
    private lateinit var userIdsByTenantIdAndRoleCodeCacheHandler: io.kudos.ams.auth.provider.cache.UserIdsByTenantIdAndRoleCodeCacheHandler

    override fun getRoleById(id: String): AuthRoleCacheItem? {
        return roleByIdCacheHandler.getRoleById(id)
    }

    override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheItem> {
        return roleByIdCacheHandler.getRolesByIds(ids)
    }

    override fun getRoleId(tenantId: String, code: String): String? {
        return roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, code)
    }

    override fun getRoleUsers(roleId: String): List<UserAccountCacheItem> {
        return authRoleService.getRoleUsers(roleId)
    }

    override fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String> {
        return userIdsByTenantIdAndRoleCodeCacheHandler.getUserIds(tenantId, roleCode)
    }

    override fun getRoleResources(roleId: String): List<SysResourceCacheItem> {
        return authRoleService.getRoleResources(roleId)
    }

    override fun hasResource(roleId: String, resourceId: String): Boolean {
        return authRoleService.hasResource(roleId, resourceId)
    }

    override fun getRoleIds(tenantId: String): List<String> {
        return authRoleService.getRoleIds(tenantId)
    }

    override fun getResources(userId: String): List<SysResourceCacheItem> {
        return authRoleService.getResources(userId)
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheItem> {
        return authRoleService.getUserRoles(userId)
    }

    override fun hasRole(userId: String, roleId: String): Boolean {
        return authRoleService.hasRole(userId, roleId)
    }

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean {
        return authRoleService.hasRoleByCode(userId, tenantId, roleCode)
    }

    override fun isUserHasResource(userId: String, resourceId: String): Boolean {
        return authRoleService.hasResource(userId, resourceId)
    }

    //endregion your codes 2

}
