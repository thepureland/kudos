package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.assurance.AuthenticationAssuranceVerifier
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialRenameRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderProperties
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinish
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinishCommand
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinishRequest
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationStart
import io.kudos.ms.auth.provider.webauthn.service.WebAuthnRegistrationService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Current-user WebAuthn credential enrollment and management boundary. */
@RestController
@RequestMapping("/api/public/auth/mfa/webauthn")
@ConditionalOnProperty(prefix = "kudos.ms.auth.webauthn", name = ["enabled"], havingValue = "true")
open class WebAuthnEnrollmentPublicController(
    private val registrationService: WebAuthnRegistrationService,
    private val credentialService: IWebAuthnCredentialService,
    private val assuranceVerifier: AuthenticationAssuranceVerifier,
    private val properties: WebAuthnProviderProperties,
) {

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @GetMapping("/credentials")
    open fun credentials(request: HttpServletRequest): List<WebAuthnCredentialSummary> {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return credentialService.listActive(principal.tenantId, principal.id)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @PostMapping("/registrations")
    open fun begin(request: HttpServletRequest): WebAuthnRegistrationStart {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return registrationService.begin(principal.id, principal.tenantId)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @PostMapping("/registrations/{id}/finish")
    open fun finish(
        @PathVariable id: String,
        @RequestBody body: WebAuthnRegistrationFinishRequest,
        request: HttpServletRequest,
    ): WebAuthnRegistrationFinish {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return registrationService.finish(
            userId = principal.id,
            tenantId = principal.tenantId,
            command = WebAuthnRegistrationFinishCommand(
                ceremonyId = id,
                credentialResponseJson = body.credentialResponseJson,
                displayName = body.displayName,
            ),
        )
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @PatchMapping("/credentials/{credentialId}")
    open fun rename(
        @PathVariable credentialId: String,
        @RequestBody body: WebAuthnCredentialRenameRequest,
        request: HttpServletRequest,
    ): WebAuthnCredentialSummary {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return credentialService.rename(principal.tenantId, principal.id, credentialId, body.displayName)
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    @DeleteMapping("/credentials/{credentialId}")
    open fun revoke(
        @PathVariable credentialId: String,
        request: HttpServletRequest,
    ): Boolean {
        val principal = currentPrincipal(request)
        requireRecentAuthentication(principal, request)
        return credentialService.revoke(principal.tenantId, principal.id, credentialId)
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
