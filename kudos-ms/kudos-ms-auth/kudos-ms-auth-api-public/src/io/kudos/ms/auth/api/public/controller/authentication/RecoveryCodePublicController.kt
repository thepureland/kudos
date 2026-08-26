package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeSet
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeStatus
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.recovery.RecoveryCodeProperties
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Current-user recovery-code lifecycle. Raw codes are emitted only by [generate]. */
@RestController
@RequestMapping("/api/public/auth/mfa/recovery-codes")
open class RecoveryCodePublicController(
    private val recoveryCodeService: IRecoveryCodeService,
    private val assuranceVerifier: AuthenticationAssuranceVerifier,
    private val properties: RecoveryCodeProperties,
) {

    @GetMapping
    open fun status(request: HttpServletRequest): RecoveryCodeStatus {
        val principal = currentPrincipal(request)
        return recoveryCodeService.status(principal.id, principal.tenantId)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @PostMapping
    open fun generate(request: HttpServletRequest): RecoveryCodeSet {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return recoveryCodeService.generate(principal.id, principal.tenantId)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @DeleteMapping
    open fun revoke(request: HttpServletRequest): Boolean {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return recoveryCodeService.revoke(principal.id, principal.tenantId)
    }

    private fun currentPrincipal(request: HttpServletRequest): SessionUserPrincipal =
        request.getSession(false)?.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "A local Kudos session is required")

    private fun requireRecentAuthentication(principal: SessionUserPrincipal, request: HttpServletRequest) {
        val session = (request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as? AuthenticationSession)
            ?.takeIf { it.userId == principal.id && it.tenantId == principal.tenantId }
        assuranceVerifier.verify(
            session,
            DefaultAuthenticationAssurancePolicy.ACR_PASSWORD,
            properties.operationReauthenticationMaxAgeSeconds.coerceAtLeast(1),
        )
    }
}
