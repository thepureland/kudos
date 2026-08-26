package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderClaimMappingAdminSaveRequest
import io.kudos.ms.auth.common.provider.vo.response.IdentityProviderClaimMappingAdminResponse
import io.kudos.ms.auth.core.provider.claim.model.EffectiveIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingException
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingSaveCommand
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/auth/identityProviderClaimMapping")
open class IdentityProviderClaimMappingAdminController(
    private val service: IIdentityProviderClaimMappingService,
) {

    @GetMapping("/get")
    @RequiresPermission("auth:identity-provider-claim:view")
    open fun get(@RequestParam providerId: String): IdentityProviderClaimMappingAdminResponse = translateErrors {
        val operator = currentOperator()
        service.getEffective(providerId, operator.tenantId).toResponse()
    }

    @PostMapping("/save")
    @RequiresPermission("auth:identity-provider-claim:update")
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存外部身份 Provider claim mapping",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(
        @RequestBody request: IdentityProviderClaimMappingAdminSaveRequest,
    ): IdentityProviderClaimMappingAdminResponse = translateErrors {
        val operator = currentOperator()
        service.save(
            IdentityProviderClaimMappingSaveCommand(
                providerId = request.providerId,
                tenantId = operator.tenantId,
                subjectClaims = request.subjectClaims,
                usernameClaims = request.usernameClaims,
                displayNameClaims = request.displayNameClaims,
                emailClaims = request.emailClaims,
                emailVerifiedClaims = request.emailVerifiedClaims,
                phoneClaims = request.phoneClaims,
                phoneVerifiedClaims = request.phoneVerifiedClaims,
                avatarClaims = request.avatarClaims,
                localeClaims = request.localeClaims,
                unionIdClaims = request.unionIdClaims,
                actorUserId = operator.id,
                operationReason = request.reason,
            )
        ).toResponse()
    }

    private fun EffectiveIdentityProviderClaimMapping.toResponse() =
        IdentityProviderClaimMappingAdminResponse(
            providerId, subjectClaims, usernameClaims, displayNameClaims, emailClaims, emailVerifiedClaims,
            phoneClaims, phoneVerifiedClaims, avatarClaims, localeClaims, unionIdClaims, configured,
        )

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: IdentityProviderClaimMappingException) {
        val status = when (e.errorCode) {
            "EXTERNAL_PROVIDER_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND
            "EXTERNAL_PROVIDER_TENANT_MISMATCH" -> HttpStatus.FORBIDDEN
            "EXTERNAL_PROVIDER_TEMPLATE_NOT_AVAILABLE" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
    }
}
