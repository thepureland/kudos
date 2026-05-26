package io.kudos.ms.auth.client.role.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.auth.client.role.proxy.IAuthRoleProxy
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.stereotype.Component


/**
 * Feign fallback implementation for the role proxy.
 *
 * **Security-relevant**: [hasResource] / [hasRole] / [hasRoleByCode] / [isUserHasResource]
 * always return `false` (deny) when the remote is unreachable. This is the secure default —
 * fail-closed rather than fail-open. If a caller needs to allow through when auth is down
 * (e.g. during bootstrap or to skip health checks), it must implement its own fail-open bypass.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class AuthRoleFallback :
    AbstractFeignFallbackSupport("AuthRoleFallback"), IAuthRoleProxy {

    override fun getRoleById(id: String): AuthRoleCacheEntry? {
        warnRead("getRoleById", id)
        return null
    }

    override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> {
        warnRead("getRolesByIds", ids)
        return emptyMap()
    }

    override fun getRoleId(tenantId: String, code: String): String? {
        warnRead("getRoleId", tenantId, code)
        return null
    }

    override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> {
        warnRead("getRoleUsers", roleId)
        return emptyList()
    }

    override fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String> {
        warnRead("getUserIdsByRoleCode", tenantId, roleCode)
        return emptyList()
    }

    override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> {
        warnRead("getRoleResources", roleId)
        return emptyList()
    }

    override fun hasResource(roleId: String, resourceId: String): Boolean {
        warnRead("hasResource", roleId, resourceId)
        return false
    }

    override fun getRoleIds(tenantId: String): List<String> {
        warnRead("getRoleIds", tenantId)
        return emptyList()
    }

    override fun getResources(userId: String): List<SysResourceCacheEntry> {
        warnRead("getResources", userId)
        return emptyList()
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> {
        warnRead("getUserRoles", userId)
        return emptyList()
    }

    override fun hasRole(userId: String, roleId: String): Boolean {
        warnRead("hasRole", userId, roleId)
        return false
    }

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean {
        warnRead("hasRoleByCode", userId, tenantId, roleCode)
        return false
    }

    override fun isUserHasResource(userId: String, resourceId: String): Boolean {
        warnRead("isUserHasResource", userId, resourceId)
        return false
    }
}
