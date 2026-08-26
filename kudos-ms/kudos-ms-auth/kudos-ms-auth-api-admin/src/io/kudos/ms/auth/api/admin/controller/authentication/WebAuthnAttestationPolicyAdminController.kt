package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.mfa.webauthn.attestation.vo.WebAuthnAttestationPolicyAdminResponse
import io.kudos.ms.auth.common.authentication.mfa.webauthn.attestation.vo.WebAuthnAttestationPolicyAdminSaveRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.EffectiveWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAttestationPolicyService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound management API for WebAuthn authenticator admission policy. */
@RestController
@RequestMapping("/api/admin/auth/webauthn/attestationPolicy")
open class WebAuthnAttestationPolicyAdminController(
    private val service: IWebAuthnAttestationPolicyService,
) {

    @GetMapping("/get")
    @RequiresPermission(VIEW_PERMISSION)
    open fun get(): WebAuthnAttestationPolicyAdminResponse = translateErrors {
        val operator = currentOperator()
        service.getEffective(operator.tenantId).toResponse()
    }

    @PostMapping("/save")
    @RequiresPermission(UPDATE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存租户 WebAuthn 认证器准入策略",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(
        @RequestBody request: WebAuthnAttestationPolicyAdminSaveRequest,
    ): WebAuthnAttestationPolicyAdminResponse = translateErrors {
        val operator = currentOperator()
        service.save(
            WebAuthnAttestationPolicySaveCommand(
                tenantId = operator.tenantId,
                aaguidMode = request.aaguidMode,
                aaguids = request.aaguids,
                allowedAttestationFormats = request.allowedAttestationFormats,
                requireTrustedAttestation = request.requireTrustedAttestation,
                actorUserId = operator.id,
                operationReason = request.reason,
            )
        ).toResponse()
    }

    private fun EffectiveWebAuthnAttestationPolicy.toResponse() = WebAuthnAttestationPolicyAdminResponse(
        aaguidMode = aaguidMode.name,
        aaguids = aaguids,
        allowedAttestationFormats = allowedAttestationFormats,
        requireTrustedAttestation = requireTrustedAttestation,
        trustSourceAvailable = service.isTrustedAttestationAvailable(),
        effectiveFrom = effectiveFrom,
        configured = configured,
    )

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: WebAuthnAttestationPolicyException) {
        val status = when (e.errorCode) {
            "WEBAUTHN_ATTESTATION_POLICY_UPDATE_FAILED",
            "WEBAUTHN_ATTESTATION_TRUST_NOT_AVAILABLE",
            -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
        const val VIEW_PERMISSION = "auth:webauthn-attestation-policy:view"
        const val UPDATE_PERMISSION = "auth:webauthn-attestation-policy:update"
    }
}
