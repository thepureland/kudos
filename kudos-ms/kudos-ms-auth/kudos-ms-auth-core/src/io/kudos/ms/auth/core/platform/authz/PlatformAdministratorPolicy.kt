package io.kudos.ms.auth.core.platform.authz

import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * Single source of truth for the platform-administrator identity.
 *
 * Code matching alone is unsafe in a tenant-scoped role namespace. A privileged role must be in a
 * deployment-configured platform tenant, be built in (therefore protected from ordinary mutation),
 * and have a zero delegation ceiling.
 *
 * @author K
 * @author AI: Codex
 */
@Component
open class PlatformAdministratorPolicy {

    @Resource
    private lateinit var properties: AuthzProperties

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    open fun isPlatformAdministrator(subject: SubjectRef): Boolean =
        isPlatformAdministrator(subject.principalId)

    open fun isPlatformAdministrator(principalId: String): Boolean {
        if (principalId.isBlank()) return false
        val adminCodes = properties.platformAdminRoleCodes
        val adminTenants = properties.platformAdminTenantIds
        if (adminCodes.isEmpty() || adminTenants.isEmpty()) return false

        val roleIds = roleIdsByUserIdCache.getRoleIds(principalId)
        if (roleIds.isEmpty()) return false
        return authRoleHashCache.getRolesByIds(roleIds).values.any { role ->
            role.code in adminCodes &&
                role.tenantId in adminTenants &&
                role.builtIn == true &&
                role.delegableMax == 0
        }
    }

    open fun isReservedRoleCode(code: String): Boolean =
        code.isNotBlank() && code in properties.platformAdminRoleCodes
}
