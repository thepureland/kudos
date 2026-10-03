package io.kudos.ms.auth.core.organization.service

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.stereotype.Component

/**
 * Where an organization account's request works, read from the verified session principal.
 *
 * Organization accounts always work in exactly one scope per session: a tenant (with a sub-system),
 * or the organization itself (management only; no tenant). The scope is a candidate that every
 * request re-validates; it never changes the account's identity.
 */
sealed interface OrganizationRequestTarget {
    val organizationId: String
    val userId: String

    data class Tenant(
        override val organizationId: String,
        override val userId: String,
        val tenantId: String,
        val subSystemCode: String?,
    ) : OrganizationRequestTarget

    data class Organization(override val organizationId: String, override val userId: String) : OrganizationRequestTarget
}

/**
 * Resolves the current [OrganizationRequestTarget]. Null in legacy mode, for legacy principals, and for
 * trusted internal work.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationRequestTargetResolver(private val mode: OrganizationMode) {

    open fun current(): OrganizationRequestTarget? {
        if (!mode.enabled) return null
        val principal = CurrentUserKit.currentPrincipalOrNull() ?: return null
        val organizationId = principal.organizationId?.takeIf(String::isNotBlank) ?: return null
        if (principal.tenantId.isBlank()) return OrganizationRequestTarget.Organization(organizationId, principal.id)
        val subSystemCode = principal.subSystemCode?.takeIf(String::isNotBlank)
            ?: KudosContextHolder.get().subSystemCode?.takeIf(String::isNotBlank)
        return OrganizationRequestTarget.Tenant(organizationId, principal.id, principal.tenantId, subSystemCode)
    }

    /** The current target when it belongs to [userId] (questions about someone else get none). */
    open fun forUser(userId: String): OrganizationRequestTarget? = current()?.takeIf { it.userId == userId }
}
