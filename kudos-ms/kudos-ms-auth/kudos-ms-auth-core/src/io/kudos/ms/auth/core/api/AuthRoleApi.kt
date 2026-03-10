package io.kudos.ms.auth.core.api

import io.kudos.ms.auth.common.api.IAuthRoleApi
import io.kudos.ms.auth.common.vo.role.AuthRoleCacheEntry
import io.kudos.ms.auth.core.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.cache.UserIdsByTenantIdAndRoleCodeCache
import io.kudos.ms.auth.core.service.iservice.IAuthRoleService
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.user.common.vo.user.UserAccountCacheEntry
import jakarta.annotation.Resource
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

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleService: IAuthRoleService

    @Resource
    private lateinit var userIdsByTenantIdAndRoleCodeCache: UserIdsByTenantIdAndRoleCodeCache

    override fun getRoleById(id: String): AuthRoleCacheEntry? {
        return authRoleHashCache.getRoleById(id)
    }

    override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> {
        return authRoleHashCache.getRolesByIds(ids)
    }

    override fun getRoleId(tenantId: String, code: String): String? {
        return authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, code)?.id
    }

    override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> {
        return authRoleService.getRoleUsers(roleId)
    }

    override fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String> {
        return userIdsByTenantIdAndRoleCodeCache.getUserIds(tenantId, roleCode)
    }

    override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> {
        return authRoleService.getRoleResources(roleId)
    }

    override fun hasResource(roleId: String, resourceId: String): Boolean {
        return authRoleService.hasResource(roleId, resourceId)
    }

    override fun getRoleIds(tenantId: String): List<String> {
        return authRoleService.getRoleIds(tenantId)
    }

    override fun getResources(userId: String): List<SysResourceCacheEntry> {
        return authRoleService.getResources(userId)
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> {
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
