package io.kudos.ms.auth.api.public.filter

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/** Validates auth-issued browser sessions, refreshes idle activity and populates KudosContext. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 200)
open class AuthenticationSessionWebFilter(
    private val sessionService: IAuthenticationSessionService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val httpSession = request.getSession(false)
        val authSessionId = httpSession
            ?.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
        if (httpSession != null && authSessionId != null) {
            val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            val authSession = sessionService.touch(authSessionId)
            if (principal != null &&
                authSession?.userId == principal.id &&
                authSession.tenantId == principal.tenantId
            ) {
                request.setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, authSession)
                KudosContextHolder.get().user = principal
            } else {
                // This makes registry revocation authoritative even before a distributed servlet-session
                // repository physically deletes the remote session entry.
                httpSession.invalidate()
            }
        }
        // Sessions created by the legacy Passport endpoint do not carry an auth-session id and
        // remain the responsibility of UserContextWebFilter during the compatibility period.
        filterChain.doFilter(request, response)
    }
}
