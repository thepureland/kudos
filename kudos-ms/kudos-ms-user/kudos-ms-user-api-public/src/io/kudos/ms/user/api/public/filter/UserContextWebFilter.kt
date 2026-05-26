package io.kudos.ms.user.api.public.filter

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


/**
 * Pushes the [SessionUserPrincipal] stored in `HttpSession[SESSION_KEY_USER]` into [KudosContext.user].
 *
 * Must run **after** `io.kudos.ability.web.springmvc.filter.WebContextInitFilter`, which creates the
 * [KudosContext] instance and writes it into ThreadLocal; this filter only fills in the `user` field.
 * Ordering uses [Order]: `WebContextInitFilter` defaults to a non-primary Spring auto-configured order (>=0),
 * so we use `Ordered.LOWEST_PRECEDENCE - 100` for a relatively late position.
 *
 * **Does not auto-invalidate session**: logout policy is triggered explicitly by `PassportPublicController.logout`;
 * this filter only does a one-way "read session -> write context".
 *
 * @author K
 * @since 1.0.0
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
open class UserContextWebFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // session=false: do not force-create an empty session for this request; only existing logged-in sessions are read
        val session = request.getSession(false)
        val raw = session?.getAttribute(KudosContext.SESSION_KEY_USER)
        if (raw is SessionUserPrincipal) {
            // KudosContextHolder.get() auto-creates an empty context and writes it into ThreadLocal (compatible
            // even if WebContextInitFilter did not run first); we only fill in the user field.
            KudosContextHolder.get().user = raw
        }
        filterChain.doFilter(request, response)
    }
}
