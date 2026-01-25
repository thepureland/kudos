package io.kudos.ams.auth.provider.user.api

import io.kudos.ams.auth.common.api.IAuthUserApi
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.user.cache.UserByIdCacheHandler
import io.kudos.ams.auth.provider.user.cache.UserIdByTenantIdAndUsernameCacheHandler
import io.kudos.ams.auth.provider.user.service.iservice.IAuthUserService
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 用户 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@Service
open class AuthUserApi : IAuthUserApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var userIdByTenantIdAndUsernameCacheHandler: UserIdByTenantIdAndUsernameCacheHandler

    @Autowired
    private lateinit var authUserService: IAuthUserService

    override fun getUserById(id: String): AuthUserCacheItem? {
        return userByIdCacheHandler.getUserById(id)
    }

    override fun getUsersByIds(ids: Collection<String>): Map<String, AuthUserCacheItem> {
        return userByIdCacheHandler.getUsersByIds(ids)
    }

    override fun getUserId(tenantId: String, username: String): String? {
        return userIdByTenantIdAndUsernameCacheHandler.getUserId(tenantId, username)
    }

    override fun getResources(userId: String): List<SysResourceCacheItem> {
        return authUserService.getResources(userId)
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheItem> {
        return authUserService.getUserRoles(userId)
    }

    override fun getUserDepts(userId: String): List<AuthDeptCacheItem> {
        return authUserService.getUserDepts(userId)
    }

    override fun hasRole(userId: String, roleId: String): Boolean {
        return authUserService.hasRole(userId, roleId)
    }

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean {
        return authUserService.hasRoleByCode(userId, tenantId, roleCode)
    }

    override fun isUserInDept(userId: String, deptId: String): Boolean {
        return authUserService.isUserInDept(userId, deptId)
    }

    override fun hasResource(userId: String, resourceId: String): Boolean {
        return authUserService.hasResource(userId, resourceId)
    }

    override fun getUserIds(tenantId: String): List<String> {
        return authUserService.getUserIds(tenantId)
    }

    //endregion your codes 2

}
