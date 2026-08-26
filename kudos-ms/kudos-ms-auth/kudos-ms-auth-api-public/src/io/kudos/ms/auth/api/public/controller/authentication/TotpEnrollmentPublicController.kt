package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.TotpEnrollmentChallenge
import io.kudos.ms.auth.common.authentication.vo.TotpEnrollmentStatus
import io.kudos.ms.auth.common.authentication.vo.request.ConfirmTotpEnrollmentRequest
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollmentProperties
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Current-user TOTP enrollment lifecycle. Pending secrets become active only after confirmation. */
@RestController
@RequestMapping("/api/public/auth/mfa/totp")
open class TotpEnrollmentPublicController(
    private val enrollmentService: ITotpEnrollmentService,
    private val assuranceVerifier: AuthenticationAssuranceVerifier,
    private val properties: TotpEnrollmentProperties,
) {

    @GetMapping
    open fun status(request: HttpServletRequest): TotpEnrollmentStatus {
        val principal = currentPrincipal(request)
        return TotpEnrollmentStatus(enrollmentService.isEnabled(principal.id, principal.tenantId))
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @PostMapping("/enrollments")
    open fun begin(request: HttpServletRequest): TotpEnrollmentChallenge {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return enrollmentService.begin(principal.id, principal.tenantId, principal.username)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @PostMapping("/enrollments/{id}/confirm")
    open fun confirm(
        @PathVariable id: String,
        @RequestBody body: ConfirmTotpEnrollmentRequest,
        request: HttpServletRequest,
    ): Boolean {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return enrollmentService.confirm(id, principal.id, principal.tenantId, body.code)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @DeleteMapping("/enrollments/{id}")
    open fun cancel(@PathVariable id: String, request: HttpServletRequest): Boolean {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return enrollmentService.cancel(id, principal.id, principal.tenantId)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @DeleteMapping
    open fun disable(request: HttpServletRequest): Boolean {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return enrollmentService.disable(principal.id, principal.tenantId)
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
