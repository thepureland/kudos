package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderJitConfigAdminSaveRequest
import io.kudos.ms.auth.common.provider.vo.response.IdentityProviderJitConfigAdminResponse
import io.kudos.ms.auth.core.provider.jit.model.EffectiveIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigException
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigSaveCommand
import io.kudos.ms.auth.core.provider.jit.service.iservice.IIdentityProviderJitConfigService
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
@RequestMapping("/api/admin/auth/identityProviderJitConfig")
open class IdentityProviderJitConfigAdminController(
    private val service: IIdentityProviderJitConfigService,
) {

    @GetMapping("/get")
    @RequiresPermission("auth:identity-provider-jit:view")
    open fun get(@RequestParam providerId: String): IdentityProviderJitConfigAdminResponse = translateErrors {
        val operator = currentOperator()
        service.getEffective(providerId, operator.tenantId).toResponse()
    }

    @PostMapping("/save")
    @RequiresPermission("auth:identity-provider-jit:update")
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存外部身份 Provider JIT 开户配置",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(
        @RequestBody request: IdentityProviderJitConfigAdminSaveRequest,
    ): IdentityProviderJitConfigAdminResponse = translateErrors {
        val operator = currentOperator()
        service.save(
            IdentityProviderJitConfigSaveCommand(
                providerId = request.providerId,
                tenantId = operator.tenantId,
                usernameStrategy = request.usernameStrategy,
                requireVerifiedEmail = request.requireVerifiedEmail,
                allowedEmailDomains = request.allowedEmailDomains,
                defaultOrgId = request.defaultOrgId,
                defaultSupervisorId = request.defaultSupervisorId,
                accountTypeDictCode = request.accountTypeDictCode,
                accountStatusDictCode = request.accountStatusDictCode,
                defaultLocale = request.defaultLocale,
                defaultTimezone = request.defaultTimezone,
                defaultCurrency = request.defaultCurrency,
                actorUserId = operator.id,
                operationReason = request.reason,
            )
        ).toResponse()
    }

    private fun EffectiveIdentityProviderJitConfig.toResponse() = IdentityProviderJitConfigAdminResponse(
        providerId = providerId,
        usernameStrategy = usernameStrategy.name,
        requireVerifiedEmail = requireVerifiedEmail,
        allowedEmailDomains = allowedEmailDomains,
        defaultOrgId = defaultOrgId,
        defaultSupervisorId = defaultSupervisorId,
        accountTypeDictCode = accountTypeDictCode,
        accountStatusDictCode = accountStatusDictCode,
        defaultLocale = defaultLocale,
        defaultTimezone = defaultTimezone,
        defaultCurrency = defaultCurrency,
        configured = configured,
    )

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: IdentityProviderJitConfigException) {
        val status = when (e.errorCode) {
            "EXTERNAL_PROVIDER_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND
            "EXTERNAL_PROVIDER_TENANT_MISMATCH",
            "EXTERNAL_JIT_DEFAULT_ORG_TENANT_MISMATCH",
            "EXTERNAL_JIT_DEFAULT_SUPERVISOR_TENANT_MISMATCH" -> HttpStatus.FORBIDDEN
            "EXTERNAL_PROVIDER_NOT_JIT_CREATE",
            "EXTERNAL_JIT_DEFAULT_ORG_NOT_AVAILABLE",
            "EXTERNAL_JIT_DEFAULT_SUPERVISOR_NOT_AVAILABLE" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
    }
}
