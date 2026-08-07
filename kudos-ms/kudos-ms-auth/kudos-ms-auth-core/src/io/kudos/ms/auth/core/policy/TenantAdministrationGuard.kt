package io.kudos.ms.auth.core.policy

import io.kudos.ms.auth.core.platform.authz.PlatformAdministratorPolicy
import io.kudos.ms.auth.core.principal.PrincipalDirectoryRegistry
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * Enforces the tenant boundary on authorization administration write paths.
 *
 * @author K
 * @author AI: Codex
 */
@Component
open class TenantAdministrationGuard {

    @Resource
    private lateinit var platformAdministratorPolicy: PlatformAdministratorPolicy

    @Resource
    private lateinit var principalDirectoryRegistry: PrincipalDirectoryRegistry

    /**
     * Empty context is reserved for trusted internal/bootstrap calls. An authenticated caller may
     * manage its own tenant only, unless it satisfies the hardened platform-administrator policy.
     */
    open fun assertCanManage(targetTenantId: String) {
        require(targetTenantId.isNotBlank()) { "target tenant id must not be blank." }
        val current = CurrentUserKit.currentPrincipalOrNull() ?: return
        require(current.tenantId == targetTenantId || platformAdministratorPolicy.isPlatformAdministrator(current.id)) {
            "cross-tenant authorization administration is forbidden: caller tenant=${current.tenantId}, target=$targetTenantId."
        }
    }

    /** Platform administrator role codes are deployment-reserved and cannot be minted by tenants. */
    open fun assertRoleCodeManageable(code: String, tenantId: String) {
        assertCanManage(tenantId)
        val current = CurrentUserKit.currentPrincipalOrNull() ?: return
        require(!platformAdministratorPolicy.isReservedRoleCode(code) ||
            platformAdministratorPolicy.isPlatformAdministrator(current.id)) {
            "role code $code is reserved for platform administration."
        }
    }

    /** A group membership may never cross the group's tenant boundary. */
    open fun assertPrincipalBelongsToTenant(principalId: String, principalType: String?, tenantId: String) {
        val principal = principalDirectoryRegistry.find(principalId, principalType)
            ?: throw IllegalArgumentException("principal not found: $principalId")
        require(principal.active) { "principal $principalId is inactive." }
        require(principal.tenantId == tenantId) {
            "principal $principalId belongs to tenant ${principal.tenantId}, not $tenantId."
        }
    }
}
