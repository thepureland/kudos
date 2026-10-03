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
                authSession.tenantId == principal.tenantId &&
                authSession.organizationId == principal.organizationId &&
                authSession.subSystemCode == principal.subSystemCode
            ) {
                // A page loaded before a context switch must not submit into the new context.
                val expectedContext = request.getHeader(CONTEXT_VERSION_HEADER)
                if (expectedContext != null && expectedContext != authSession.id) {
                    response.status = HttpServletResponse.SC_CONFLICT
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write("""{"code":"AUTHENTICATION_CONTEXT_CHANGED","message":"The session context changed; reload before submitting"}""")
                    return
                }
                request.setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, authSession)
                request.setAttribute(SessionUserPrincipal.VALIDATED_REQUEST_ATTRIBUTE, principal)
                KudosContextHolder.get().user = principal
                // Organization sessions work in the session's tenant (none in the organization scope) and
                // sub-system; legacy sessions keep the context exactly as before.
                if (authSession.organizationId != null) {
                    KudosContextHolder.get().apply {
                        tenantId = authSession.tenantId
                        _datasourceTenantId = authSession.tenantId.takeIf(String::isNotBlank)
                        subSystemCode = authSession.subSystemCode
                    }
                }
            } else {
                // This makes registry revocation authoritative even before a distributed servlet-session
                // repository physically deletes the remote session entry.
                httpSession.invalidate()
                KudosContextHolder.getOrNull()?.user = null
            }
        }
        if (httpSession != null && authSessionId == null &&
            httpSession.getAttribute(KudosContext.SESSION_KEY_USER) != null
        ) {
            httpSession.invalidate()
            KudosContextHolder.getOrNull()?.user = null
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        /** Sent by the console with the session id it rendered for; a mismatch means a stale page. */
        const val CONTEXT_VERSION_HEADER = "X-Kudos-Context-Version"
    }
}
