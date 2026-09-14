package io.kudos.ms.tag.core.security

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.springframework.stereotype.Component

class TagAccessDeniedException(message: String) : SecurityException(message)

/** Enforces tenant isolation for both embedded API calls and HTTP-backed calls. */
@Component
open class TagTenantAccessGuard {
    open fun requireTenant(tenantId: String) {
        if (tenantId.isBlank()) deny("Tag tenant id must not be blank.")
        val context = currentContext()
        if (hasPlatformManagementAuthority(context)) return
        if (tenantId == PLATFORM_TENANT_ID) deny("Platform tag scope requires platform-management authority.")
        if (context.tenantId != tenantId) {
            deny("Cross-tenant tag access is forbidden.")
        }
    }

    open fun hasPlatformManagementAuthority(): Boolean =
        KudosContextHolder.getOrNull()?.let(::hasPlatformManagementAuthority) == true

    open fun requirePlatformManagement() {
        if (!hasPlatformManagementAuthority()) deny("Platform tag management authority is required.")
    }

    private fun currentContext(): KudosContext =
        KudosContextHolder.getOrNull() ?: deny("A Kudos security context is required for tag access.")

    private fun hasPlatformManagementAuthority(context: KudosContext): Boolean =
        context.otherInfos?.get(PLATFORM_MANAGEMENT_AUTHORITY) == true

    private fun deny(message: String): Nothing = throw TagAccessDeniedException(message)

    companion object {
        const val PLATFORM_TENANT_ID = "__platform__"
        const val PLATFORM_MANAGEMENT_AUTHORITY = "tag.platform-management"
    }
}
