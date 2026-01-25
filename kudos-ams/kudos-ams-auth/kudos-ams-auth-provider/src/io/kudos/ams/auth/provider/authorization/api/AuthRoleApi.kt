package io.kudos.ams.auth.provider.authorization.api

import io.kudos.ams.auth.common.api.IAuthRoleApi
import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.authorization.cache.RoleByIdCacheHandler
import io.kudos.ams.auth.provider.authorization.cache.RoleIdByTenantIdAndRoleCodeCacheHandler
import io.kudos.ams.auth.provider.authorization.cache.UserIdsByTenantIdAndRoleCodeCacheHandler
import io.kudos.ams.auth.provider.authorization.service.iservice.IAuthRoleService
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
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
    private lateinit var roleByIdCacheHandler: RoleByIdCacheHandler

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    @Autowired
    private lateinit var authRoleService: IAuthRoleService

    @Autowired
    private lateinit var userIdsByTenantIdAndRoleCodeCacheHandler: UserIdsByTenantIdAndRoleCodeCacheHandler

    override fun getRoleById(id: String): AuthRoleCacheItem? {
        return roleByIdCacheHandler.getRoleById(id)
    }

    override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheItem> {
        return roleByIdCacheHandler.getRolesByIds(ids)
    }

    override fun getRoleId(tenantId: String, code: String): String? {
        return roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, code)
    }

    override fun getRoleUsers(roleId: String): List<AuthUserCacheItem> {
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

    //endregion your codes 2

}
