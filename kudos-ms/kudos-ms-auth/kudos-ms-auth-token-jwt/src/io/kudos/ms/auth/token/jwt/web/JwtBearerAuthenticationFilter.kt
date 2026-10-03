package io.kudos.ms.auth.token.jwt.web

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.token.jwt.model.AccessTokenException
import io.kudos.ms.auth.token.jwt.model.IJwtAccessTokenService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter

/** Validates Kudos Bearer tokens and exposes their trusted subject through KudosContext. */
@Order(Ordered.LOWEST_PRECEDENCE - 300)
open class JwtBearerAuthenticationFilter(
    private val accessTokenService: IJwtAccessTokenService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorization = request.getHeader(AUTHORIZATION_HEADER)
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }
        val rawToken = authorization.substring(BEARER_PREFIX.length).trim()
        if (rawToken.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        try {
            val session = accessTokenService.verify(rawToken)
            request.setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, session)
            KudosContextHolder.get().user = SessionUserPrincipal(
                id = session.userId,
                tenantId = session.tenantId,
                username = session.username.orEmpty(),
                organizationId = session.organizationId,
                subSystemCode = session.subSystemCode,
            )
            // Organization sessions work in the session's tenant (none in the organization scope) and
            // sub-system; legacy sessions keep the context exactly as before.
            if (session.organizationId != null) {
                KudosContextHolder.get().apply {
                    tenantId = session.tenantId
                    _datasourceTenantId = session.tenantId.takeIf(String::isNotBlank)
                    subSystemCode = session.subSystemCode
                }
            }
            filterChain.doFilter(request, response)
        } catch (_: AccessTokenException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
