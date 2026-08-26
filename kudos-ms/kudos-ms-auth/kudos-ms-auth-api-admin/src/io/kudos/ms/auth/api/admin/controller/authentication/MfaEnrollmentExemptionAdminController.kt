package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.MfaEnrollmentExemptionAdminGrantRequest
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.MfaEnrollmentExemptionAdminResponse
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.MfaEnrollmentExemptionAdminRevokeRequest
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionGrantCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionRevokeCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound, audited rescue for accounts the MFA enrollment requirement has locked out. */
@RestController
@RequestMapping("/api/admin/auth/mfaEnrollmentExemptions")
open class MfaEnrollmentExemptionAdminController(
    private val service: IMfaEnrollmentExemptionService,
    private val userAccountService: IUserAccountService,
) {

    @GetMapping("/list")
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<MfaEnrollmentExemptionAdminResponse> = translateErrors {
        val operator = currentOperator()
        if (limit !in 1..MAX_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and $MAX_LIMIT")
        }
        val targetUserId = userId?.trim()?.also {
            if (it.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank")
            requireTenantUser(it, operator.tenantId)
        }
        service.listRecent(operator.tenantId, targetUserId, limit).map { it.toResponse() }
    }

    @PostMapping("/grant")
    @RequiresPermission(GRANT_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "授予 MFA 注册临时豁免",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun grant(
        @RequestBody request: MfaEnrollmentExemptionAdminGrantRequest,
    ): MfaEnrollmentExemptionAdminResponse = translateErrors {
        val operator = currentOperator()
        val targetUserId = requireTargetUser(request.userId, operator.tenantId)
        service.grant(
            MfaEnrollmentExemptionGrantCommand(
                tenantId = operator.tenantId,
                userId = targetUserId,
                expiresAt = request.expiresAt,
                actorUserId = operator.id,
                reason = request.reason,
            )
        ).toResponse()
    }

    @PostMapping("/revoke")
    @RequiresPermission(REVOKE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "撤销 MFA 注册临时豁免",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun revoke(
        @RequestBody request: MfaEnrollmentExemptionAdminRevokeRequest,
    ): Map<String, Int> = translateErrors {
        val operator = currentOperator()
        val targetUserId = requireTargetUser(request.userId, operator.tenantId)
        mapOf(
            "revokedCount" to service.revoke(
                MfaEnrollmentExemptionRevokeCommand(
                    tenantId = operator.tenantId,
                    userId = targetUserId,
                    actorUserId = operator.id,
                    reason = request.reason,
                )
            )
        )
    }

    private fun MfaEnrollmentExemption.toResponse() = MfaEnrollmentExemptionAdminResponse(
        userId = userId,
        status = status.name,
        reason = reason,
        grantedBy = grantedBy,
        grantedAt = grantedAt,
        expiresAt = expiresAt,
        revokedBy = revokedBy,
        revokeReason = revokeReason,
        revokedAt = revokedAt,
    )

    private fun requireTargetUser(userId: String, tenantId: String): String {
        val trimmed = userId.trim()
        if (trimmed.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank")
        }
        requireTenantUser(trimmed, tenantId)
        return trimmed
    }

    private fun requireTenantUser(userId: String, tenantId: String) {
        if (userAccountService.getUserRecord(userId)?.tenantId != tenantId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
        }
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: TenantMfaPolicyException) {
        val status = when (e.errorCode) {
            // Refusals about the account's own state, not about the request being malformed.
            "MFA_EXEMPTION_ALREADY_ENROLLED",
            "MFA_EXEMPTION_NOT_REQUIRED",
            -> HttpStatus.CONFLICT
            // An administrator exempting themselves is refused outright rather than treated as bad input.
            "MFA_EXEMPTION_SELF_GRANT_FORBIDDEN" -> HttpStatus.FORBIDDEN
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth-mfa-exemption"
        const val VIEW_PERMISSION = "auth:mfa-exemption:view"
        const val GRANT_PERMISSION = "auth:mfa-exemption:grant"
        const val REVOKE_PERMISSION = "auth:mfa-exemption:revoke"
        const val MAX_LIMIT = 200
    }
}
