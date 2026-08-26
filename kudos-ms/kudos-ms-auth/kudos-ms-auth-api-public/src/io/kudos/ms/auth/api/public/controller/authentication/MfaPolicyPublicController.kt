package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.MfaPolicyStatus
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Returns the effective, secret-free MFA policy decision for the current local account. */
@RestController
@RequestMapping("/api/public/auth/mfa/policy")
open class MfaPolicyPublicController(
    private val service: ITenantMfaPolicyService,
    private val exemptionService: IMfaEnrollmentExemptionService? = null,
) {

    @GetMapping
    open fun status(request: HttpServletRequest): MfaPolicyStatus {
        val principal = currentPrincipal(request)
        val decision = service.evaluate(principal.tenantId, principal.id)
        return MfaPolicyStatus(
            mode = decision.policy.mode.name,
            required = decision.required,
            enrolled = decision.enrolled,
            enrollmentRequired = decision.enrollmentRequired,
            gracePeriodActive = decision.gracePeriodActive,
            graceExpiresAt = decision.graceExpiresAt,
            // Only shown while it actually changes anything: an enrolled account is challenged regardless, and
            // telling it that an exemption is in force would suggest otherwise.
            exemptionExpiresAt = if (decision.enrollmentRequired) {
                exemptionService?.activeExemptionExpiry(principal.tenantId, principal.id)
            } else {
                null
            },
            allowedMethods = decision.policy.allowedMethods.map { it.name }.toSortedSet(),
            recoveryCodesEnabled = decision.policy.recoveryCodesEnabled,
        )
    }

    private fun currentPrincipal(request: HttpServletRequest): SessionUserPrincipal =
        request.getSession(false)?.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "A local Kudos session is required")
}
