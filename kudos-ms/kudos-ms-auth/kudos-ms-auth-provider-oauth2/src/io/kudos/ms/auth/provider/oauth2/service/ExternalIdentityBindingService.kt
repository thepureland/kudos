package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.stereotype.Service

/** Binds only claims obtained from a verified provider callback. */
@Service
open class ExternalIdentityBindingService(
    private val bindingService: IUserAccountThirdService,
) {
    open fun bind(userId: String, tenantId: String, resolved: ResolvedExternalPrincipal): UserAccountThird {
        check(resolved.tenantId == tenantId) { "External identity tenant does not match local session" }
        val principal = resolved.principal
        return bindingService.bindExternalIdentity(
            ExternalAccountBindingCommand(
                userId = userId,
                tenantId = tenantId,
                identityProviderId = resolved.providerId,
                providerCode = resolved.providerCode,
                issuer = principal.issuer,
                subject = principal.subject,
                unionId = principal.unionId,
                displayName = principal.displayName,
                email = principal.email,
                avatarUrl = principal.avatarUrl,
            )
        )
    }
}
