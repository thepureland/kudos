package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialAuditSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound, read-only administrator view of WebAuthn credential metadata. */
@RestController
@RequestMapping("/api/admin/auth/webauthn/credentials")
open class WebAuthnCredentialAdminController(
    private val credentialService: IWebAuthnCredentialService,
    private val userAccountService: IUserAccountService,
) {

    @GetMapping
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(@RequestParam userId: String): List<WebAuthnCredentialAuditSummary> {
        val targetUserId = userId.trim()
        if (targetUserId.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank")
        }
        val operator = CurrentUserKit.currentPrincipalOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")
        val target = userAccountService.getUserRecord(targetUserId)
        if (target?.tenantId != operator.tenantId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
        }
        return credentialService.listForAudit(operator.tenantId, targetUserId)
    }

    private companion object {
        const val VIEW_PERMISSION = "auth:webauthn-credential:view"
    }
}
