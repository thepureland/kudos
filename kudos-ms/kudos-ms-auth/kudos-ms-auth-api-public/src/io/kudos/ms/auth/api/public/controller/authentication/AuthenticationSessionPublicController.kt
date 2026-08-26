package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Current-user operations for a browser authentication session. */
@RestController
@RequestMapping("/api/public/auth/sessions")
open class AuthenticationSessionPublicController(
    private val sessionService: IAuthenticationSessionService,
) {

    /** Lists active logical sessions owned by the authenticated tenant user. */
    @GetMapping
    open fun list(request: HttpServletRequest): List<AuthenticationSession> {
        val principal = request.currentPrincipal() ?: return emptyList()
        return sessionService.listForUser(principal.tenantId, principal.id)
    }

    /** Returns the current logical session without exposing the servlet session id. */
    @GetMapping("/current")
    open fun getCurrent(request: HttpServletRequest): AuthenticationSession? {
        val httpSession = request.getSession(false) ?: return null
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: return null
        val id = httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
            ?: return null
        val authSession = sessionService.get(id)
        if (authSession?.isActive() == true &&
            authSession.userId == principal.id &&
            authSession.tenantId == principal.tenantId
        ) {
            return authSession
        }
        // A missing, expired or mismatched registry entry must fail closed.
        httpSession.invalidate()
        return null
    }

    /** Revokes the registry record and invalidates the current browser session. */
    @DeleteMapping("/current")
    open fun revokeCurrent(request: HttpServletRequest): Boolean {
        val httpSession = request.getSession(false) ?: return false
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
        val id = httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
        if (principal != null && id != null) {
            sessionService.revokeForUser(id, principal.tenantId, principal.id, USER_LOGOUT)
        }
        httpSession.invalidate()
        return principal != null
    }

    /** Revokes an owned session; a remote browser is rejected by the session filter on its next request. */
    @DeleteMapping("/{id}")
    open fun revoke(@PathVariable id: String, request: HttpServletRequest): Boolean {
        val httpSession = request.getSession(false) ?: return false
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: return false
        val revoked = sessionService.revokeForUser(id, principal.tenantId, principal.id, USER_REVOKE)
            ?: return false
        val currentId = httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
        if (currentId == revoked.id) httpSession.invalidate()
        return true
    }

    private fun HttpServletRequest.currentPrincipal(): SessionUserPrincipal? =
        getSession(false)?.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal

    companion object {
        private const val USER_LOGOUT = "USER_LOGOUT"
        private const val USER_REVOKE = "USER_REVOKE"
    }
}
