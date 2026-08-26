package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.TenantMfaPolicyAdminResponse
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.TenantMfaPolicyAdminSaveRequest
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/auth/mfaPolicy")
open class TenantMfaPolicyAdminController(
    private val service: ITenantMfaPolicyService,
) {

    @GetMapping("/get")
    @RequiresPermission("auth:mfa-policy:view")
    open fun get(): TenantMfaPolicyAdminResponse = translateErrors {
        val operator = currentOperator()
        service.getEffective(operator.tenantId).toResponse()
    }

    @PostMapping("/save")
    @RequiresPermission("auth:mfa-policy:update")
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存租户多因素认证策略",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(@RequestBody request: TenantMfaPolicyAdminSaveRequest): TenantMfaPolicyAdminResponse =
        translateErrors {
            val operator = currentOperator()
            service.save(
                TenantMfaPolicySaveCommand(
                    tenantId = operator.tenantId,
                    mode = request.mode,
                    gracePeriodDays = request.gracePeriodDays,
                    allowedMethods = request.allowedMethods,
                    recoveryCodesEnabled = request.recoveryCodesEnabled,
                    requiredAccountTypeCodes = request.requiredAccountTypeCodes,
                    requiredRoleCodes = request.requiredRoleCodes,
                    actorUserId = operator.id,
                    operationReason = request.reason,
                )
            ).toResponse()
        }

    private fun EffectiveTenantMfaPolicy.toResponse() = TenantMfaPolicyAdminResponse(
        mode = mode.name,
        gracePeriodDays = gracePeriodDays,
        allowedMethods = allowedMethods.map { it.name }.toSortedSet(),
        recoveryCodesEnabled = recoveryCodesEnabled,
        requiredAccountTypeCodes = requiredAccountTypeCodes,
        requiredRoleCodes = requiredRoleCodes,
        configured = configured,
    )

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: TenantMfaPolicyException) {
        val status = when (e.errorCode) {
            "MFA_POLICY_ACCOUNT_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "MFA_POLICY_ACCOUNT_TENANT_MISMATCH" -> HttpStatus.FORBIDDEN
            "MFA_POLICY_UPDATE_FAILED" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
    }
}
