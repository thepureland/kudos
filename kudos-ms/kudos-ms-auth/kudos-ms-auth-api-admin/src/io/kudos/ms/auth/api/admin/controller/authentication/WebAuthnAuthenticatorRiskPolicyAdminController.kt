package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.mfa.webauthn.risk.vo.WebAuthnAuthenticatorRiskPolicyAdminResponse
import io.kudos.ms.auth.common.authentication.mfa.webauthn.risk.vo.WebAuthnAuthenticatorRiskPolicyAdminSaveRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.EffectiveWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.iservice.IWebAuthnAuthenticatorRiskPolicyService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound management API for WebAuthn authenticator risk enforcement. */
@RestController
@RequestMapping("/api/admin/auth/webauthn/riskPolicy")
open class WebAuthnAuthenticatorRiskPolicyAdminController(
    private val service: IWebAuthnAuthenticatorRiskPolicyService,
) {

    @GetMapping("/get")
    @RequiresPermission(VIEW_PERMISSION)
    open fun get(): WebAuthnAuthenticatorRiskPolicyAdminResponse = translateErrors {
        val operator = currentOperator()
        service.getEffective(operator.tenantId).toResponse()
    }

    @PostMapping("/save")
    @RequiresPermission(UPDATE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存租户 WebAuthn 认证器风险策略",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(
        @RequestBody request: WebAuthnAuthenticatorRiskPolicyAdminSaveRequest,
    ): WebAuthnAuthenticatorRiskPolicyAdminResponse = translateErrors {
        val operator = currentOperator()
        service.save(
            WebAuthnAuthenticatorRiskPolicySaveCommand(
                tenantId = operator.tenantId,
                blockedRiskLevels = request.blockedRiskLevels,
                actorUserId = operator.id,
                operationReason = request.reason,
            )
        ).toResponse()
    }

    private fun EffectiveWebAuthnAuthenticatorRiskPolicy.toResponse() =
        WebAuthnAuthenticatorRiskPolicyAdminResponse(
            blockedRiskLevels = blockedRiskLevels.map { it.name }.toSortedSet(),
            riskEvaluationAvailable = service.isRiskEvaluationAvailable(),
            effectiveFrom = effectiveFrom,
            configured = configured,
        )

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: WebAuthnAuthenticatorRiskPolicyException) {
        val status = when (e.errorCode) {
            "WEBAUTHN_RISK_EVALUATION_NOT_AVAILABLE",
            "WEBAUTHN_RISK_POLICY_UPDATE_FAILED",
            -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
        const val VIEW_PERMISSION = "auth:webauthn-risk-policy:view"
        const val UPDATE_PERMISSION = "auth:webauthn-risk-policy:update"
    }
}
