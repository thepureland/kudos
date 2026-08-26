package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationSessionAdminRevokeRequest
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound administrator operations for logical authentication sessions. */
@RestController
@RequestMapping("/api/admin/auth/sessions")
open class AuthenticationSessionAdminController(
    private val sessionService: IAuthenticationSessionService,
    private val userAccountService: IUserAccountService,
) {

    /** Lists the active sessions of a user in the current administrator's tenant. */
    @GetMapping
    @RequiresPermission("auth:session:view")
    open fun list(@RequestParam userId: String): List<AuthenticationSession> {
        val tenantId = resolveTargetTenant(userId)
        return sessionService.listForUser(tenantId, userId.trim())
    }

    /** Revokes one owned session; the remote browser is rejected on its next request. */
    @PostMapping("/{id}/revoke")
    @RequiresPermission("auth:session:revoke")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "管理员撤销用户会话")
    open fun revoke(
        @PathVariable id: String,
        @RequestBody request: AuthenticationSessionAdminRevokeRequest,
    ): Boolean {
        validate(id, "id")
        validateReason(request.reason)
        val userId = request.userId.trim()
        val tenantId = resolveTargetTenant(userId)
        return sessionService.revokeForUser(
            id.trim(),
            tenantId,
            userId,
            "$ADMIN_REVOKE_PREFIX${request.reason.trim()}",
        ) != null
    }

    /**
     * Resolves tenant ownership from trusted user data and deliberately returns the same response for
     * an absent user and a user in another tenant, so this endpoint cannot enumerate cross-tenant ids.
     */
    private fun resolveTargetTenant(userId: String): String {
        validate(userId, "userId")
        val operator = CurrentUserKit.currentPrincipalOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")
        val target = userAccountService.getUserRecord(userId.trim())
        val tenantId = target?.tenantId?.takeIf { it == operator.tenantId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
        return tenantId
    }

    private fun validate(value: String, name: String) {
        if (value.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$name must not be blank")
    }

    private fun validateReason(reason: String) {
        if (reason.isBlank() || reason.length > MAX_REASON_LENGTH) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "reason must contain 1 to $MAX_REASON_LENGTH characters",
            )
        }
    }

    companion object {
        private const val MODULE_CODE = "auth-session"
        private const val ADMIN_REVOKE_PREFIX = "ADMIN_REVOKE:"
        private const val MAX_REASON_LENGTH = 100
    }
}
