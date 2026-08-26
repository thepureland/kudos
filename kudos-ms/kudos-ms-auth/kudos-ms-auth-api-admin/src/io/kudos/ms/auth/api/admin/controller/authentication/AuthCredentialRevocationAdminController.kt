package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.credentialrevocation.vo.AuthCredentialRevocationAdminRequest
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationCommand
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationException
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationResult
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialTypeEnum
import io.kudos.ms.auth.core.authentication.credentialrevocation.service.iservice.IAuthCredentialRevocationService
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

/** Tenant-bound, audited revocation of a credential an account can no longer be trusted to hold. */
@RestController
@RequestMapping("/api/admin/auth/credentialRevocations")
open class AuthCredentialRevocationAdminController(
    private val service: IAuthCredentialRevocationService,
    private val userAccountService: IUserAccountService,
) {

    @GetMapping("/list")
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<AuthCredentialRevocationResult> = translateErrors {
        val operator = currentOperator()
        if (limit !in 1..MAX_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and $MAX_LIMIT")
        }
        val targetUserId = userId?.let { requireTargetUser(it, operator.tenantId) }
        service.listRecent(operator.tenantId, targetUserId, limit)
    }

    @PostMapping("/revoke")
    @RequiresPermission(REVOKE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "管理员吊销账号凭证",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun revoke(
        @RequestBody request: AuthCredentialRevocationAdminRequest,
    ): AuthCredentialRevocationResult = translateErrors {
        val operator = currentOperator()
        val targetUserId = requireTargetUser(request.userId, operator.tenantId)
        val credentialType = runCatching {
            AuthCredentialTypeEnum.valueOf(request.credentialType.trim().uppercase())
        }.getOrElse {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "credentialType must be WEBAUTHN or TOTP", it)
        }
        service.revoke(
            AuthCredentialRevocationCommand(
                tenantId = operator.tenantId,
                userId = targetUserId,
                credentialType = credentialType,
                credentialRef = request.credentialRef?.trim()?.takeIf { it.isNotEmpty() },
                actorUserId = operator.id,
                reason = request.reason,
                securityEventId = request.securityEventId?.trim()?.takeIf { it.isNotEmpty() },
            )
        )
    }

    private fun requireTargetUser(userId: String, tenantId: String): String {
        val trimmed = userId.trim()
        if (trimmed.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank")
        }
        if (userAccountService.getUserRecord(trimmed)?.tenantId != tenantId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
        }
        return trimmed
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: AuthCredentialRevocationException) {
        val status = when (e.errorCode) {
            // The credential is not this account's to revoke, or is not there at all.
            "AUTH_CREDENTIAL_REVOCATION_CREDENTIAL_NOT_FOUND",
            "AUTH_CREDENTIAL_REVOCATION_TOTP_NOT_ENROLLED",
            -> HttpStatus.NOT_FOUND
            "AUTH_CREDENTIAL_REVOCATION_ALREADY_REVOKED" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth-credential-revocation"
        const val VIEW_PERMISSION = "auth:credential-revocation:view"
        const val REVOKE_PERMISSION = "auth:credential-revocation:revoke"
        const val MAX_LIMIT = 200
    }
}
