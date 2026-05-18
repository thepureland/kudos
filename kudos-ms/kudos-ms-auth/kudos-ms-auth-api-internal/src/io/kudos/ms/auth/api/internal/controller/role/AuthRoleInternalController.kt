package io.kudos.ms.auth.api.internal.controller.role

import io.kudos.ms.auth.common.role.api.IAuthRoleApi
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.api.AuthRoleApi
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.web.bind.annotation.RestController


/**
 * 角色 内部 RPC 控制器。路径继承自 [IAuthRoleApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class AuthRoleInternalController(
    private val authRoleApi: AuthRoleApi,
) : IAuthRoleApi {

    override fun getRoleById(id: String): AuthRoleCacheEntry? =
        authRoleApi.getRoleById(id)

    override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> =
        authRoleApi.getRolesByIds(ids)

    override fun getRoleId(tenantId: String, code: String): String? =
        authRoleApi.getRoleId(tenantId, code)

    override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> =
        authRoleApi.getRoleUsers(roleId)

    override fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String> =
        authRoleApi.getUserIdsByRoleCode(tenantId, roleCode)

    override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> =
        authRoleApi.getRoleResources(roleId)

    override fun hasResource(roleId: String, resourceId: String): Boolean =
        authRoleApi.hasResource(roleId, resourceId)

    override fun getRoleIds(tenantId: String): List<String> =
        authRoleApi.getRoleIds(tenantId)

    override fun getResources(userId: String): List<SysResourceCacheEntry> =
        authRoleApi.getResources(userId)

    override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> =
        authRoleApi.getUserRoles(userId)

    override fun hasRole(userId: String, roleId: String): Boolean =
        authRoleApi.hasRole(userId, roleId)

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean =
        authRoleApi.hasRoleByCode(userId, tenantId, roleCode)

    override fun isUserHasResource(userId: String, resourceId: String): Boolean =
        authRoleApi.isUserHasResource(userId, resourceId)

}
