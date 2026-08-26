package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityAdminPrebindRequest
import io.kudos.ms.auth.common.provider.vo.request.ExternalIdentityAdminUnbindRequest
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.model.AdminExternalAccountBindingCommand
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Audited administrator lifecycle for pre-provisioned external identities. */
@RestController
@RequestMapping("/api/admin/auth/externalIdentity")
open class ExternalIdentityAdminController(
    private val identityProviderDao: AuthIdentityProviderDao,
    private val providerTemplateDao: AuthProviderTemplateDao,
    private val bindingService: IUserAccountThirdService,
) {

    @PostMapping("/prebind")
    @RequiresPermission("auth:external-identity:prebind")
    open fun prebind(@RequestBody request: ExternalIdentityAdminPrebindRequest): String {
        validate(request.userId, "userId")
        validate(request.providerId, "providerId")
        validate(request.subject, "subject")
        validateReason(request.reason)
        val operator = currentOperator()
        val provider = identityProviderDao.findActiveById(request.providerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "External identity provider is not available")
        if (provider.tenantId != operator.tenantId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "External identity provider belongs to another tenant")
        }
        if (provider.issuer != null && request.issuer != null && provider.issuer != request.issuer) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Issuer does not match the configured provider")
        }
        val template = providerTemplateDao.get(provider.templateId)?.takeIf { it.active }
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "External provider template is not available")
        return bindingService.prebindExternalIdentity(
            AdminExternalAccountBindingCommand(
                userId = request.userId,
                tenantId = operator.tenantId,
                identityProviderId = provider.id,
                providerCode = template.code.lowercase(),
                issuer = provider.issuer ?: request.issuer,
                subject = request.subject,
                unionId = request.unionId,
                displayName = request.displayName,
                email = request.email,
                avatarUrl = request.avatarUrl,
                actorUserId = operator.id,
                operationReason = request.reason.trim(),
            )
        ).id
    }

    @PostMapping("/unbind")
    @RequiresPermission("auth:external-identity:unbind")
    open fun unbind(@RequestBody request: ExternalIdentityAdminUnbindRequest): Boolean {
        validate(request.bindingId, "bindingId")
        validateReason(request.reason)
        val operator = currentOperator()
        return bindingService.adminUnbindExternalIdentity(
            bindingId = request.bindingId,
            tenantId = operator.tenantId,
            actorUserId = operator.id,
            operationReason = request.reason.trim(),
        )
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun validate(value: String, name: String) {
        if (value.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$name must not be blank")
    }

    private fun validateReason(reason: String) {
        if (reason.isBlank() || reason.length > 512) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "reason must contain 1 to 512 characters")
        }
    }
}
