package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.AvailableOrganizationContexts
import io.kudos.ms.auth.common.authentication.vo.OrganizationContextSwitchRequest
import io.kudos.ms.auth.common.authentication.vo.OrganizationContextSwitchResult
import io.kudos.ms.auth.common.authentication.vo.OrganizationContextSwitchUser
import io.kudos.ms.auth.core.authentication.organization.OrganizationContextException
import io.kudos.ms.auth.core.authentication.organization.OrganizationContextService
import io.kudos.ms.auth.core.authentication.organization.target
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Organization mode: the scopes open to the signed-in organization account, and switching between them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
open class OrganizationContextPublicController(
    private val contexts: OrganizationContextService,
    private val sessions: IAuthenticationSessionService,
) {

    @GetMapping("/contexts")
    open fun list(request: HttpServletRequest): AvailableOrganizationContexts = contexts.contexts(current(request))

    @PostMapping("/context/switch")
    open fun switch(@RequestBody change: OrganizationContextSwitchRequest, request: HttpServletRequest): OrganizationContextSwitchResult {
        val httpSession = request.getSession(false) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val previous = current(request)
        if (httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) != previous.id) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "AUTHENTICATION_CONTEXT_CHANGED")
        }
        val replacement = contexts.switch(previous, change)
        val principal = SessionUserPrincipal(
            replacement.userId, replacement.tenantId, replacement.username.orEmpty(),
            replacement.organizationId, replacement.subSystemCode,
        )
        try {
            request.changeSessionId()
            httpSession.setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, replacement.id)
            httpSession.setAttribute(KudosContext.SESSION_KEY_USER, principal)
            httpSession.setAttribute(AuthenticationSession.PRINCIPAL_INDEX_SESSION_ATTRIBUTE, replacement.userId)
        } catch (e: Exception) {
            sessions.revokeForUser(replacement.id, replacement.tenantId, replacement.userId, "CONTEXT_SWITCH_FAILED")
            httpSession.invalidate()
            throw e
        }
        return OrganizationContextSwitchResult(
            current = replacement.target(),
            user = OrganizationContextSwitchUser(principal.id, principal.username, requireNotNull(principal.organizationId), principal.tenantId),
        )
    }

    private fun current(request: HttpServletRequest): AuthenticationSession {
        val principal = request.getAttribute(SessionUserPrincipal.VALIDATED_REQUEST_ATTRIBUTE) as? SessionUserPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val observed = request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as? AuthenticationSession
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return sessions.get(observed.id)?.takeIf { it.isActive() && it.userId == principal.id && it.tenantId == principal.tenantId }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(OrganizationContextException::class)
    open fun conflict(exception: OrganizationContextException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("code" to exception.code, "message" to exception.code))
}
