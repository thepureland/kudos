package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderSecretAdminRequest
import io.kudos.ms.auth.common.provider.vo.response.IdentityProviderSecretAdminResponse
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerification
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerificationCommand
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerificationException
import io.kudos.ms.auth.provider.oauth2.secret.IdentityProviderSecretVerificationService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/auth/identityProviderSecret")
open class IdentityProviderSecretAdminController(
    private val service: IdentityProviderSecretVerificationService,
) {

    @PostMapping("/verify")
    @RequiresPermission("auth:identity-provider-secret:verify")
    @WebAudit(opType = OperationTypeEnum.QUERY, moduleCode = MODULE_CODE, desc = "检测外部身份 Provider 密钥引用")
    open fun verify(
        @RequestBody request: IdentityProviderSecretAdminRequest,
    ): IdentityProviderSecretAdminResponse = execute(request, refresh = false)

    @PostMapping("/refresh")
    @RequiresPermission("auth:identity-provider-secret:refresh")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "刷新外部身份 Provider 密钥引用")
    open fun refresh(
        @RequestBody request: IdentityProviderSecretAdminRequest,
    ): IdentityProviderSecretAdminResponse = execute(request, refresh = true)

    private fun execute(
        request: IdentityProviderSecretAdminRequest,
        refresh: Boolean,
    ): IdentityProviderSecretAdminResponse = translateErrors {
        val operator = CurrentUserKit.currentPrincipalOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")
        service.verify(
            IdentityProviderSecretVerificationCommand(
                providerId = request.providerId,
                tenantId = operator.tenantId,
                actorUserId = operator.id,
                operationReason = request.reason,
                refresh = refresh,
            )
        ).toResponse()
    }

    private fun IdentityProviderSecretVerification.toResponse() = IdentityProviderSecretAdminResponse(
        providerId = providerId,
        referenceScheme = referenceScheme,
        status = status.name,
        checkedAt = checkedAt,
    )

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: IdentityProviderSecretVerificationException) {
        val status = when (e.errorCode) {
            "EXTERNAL_PROVIDER_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND
            "EXTERNAL_PROVIDER_TENANT_MISMATCH" -> HttpStatus.FORBIDDEN
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
    }
}
