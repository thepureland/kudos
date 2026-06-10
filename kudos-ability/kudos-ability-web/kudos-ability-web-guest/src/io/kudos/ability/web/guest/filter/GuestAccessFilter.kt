package io.kudos.ability.web.guest.filter

import io.kudos.ability.web.guest.provider.IGuestAccessService
import io.kudos.ability.web.guest.provider.IGuestAccessStore
import io.kudos.base.logger.LogFactory
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Servlet filter that drives the guest-tracking lifecycle per request.
 *
 * Per-request order:
 *  1. Disabled → bypass.
 *  2. Any [io.kudos.ability.web.guest.provider.IGuestAccessIgnore] returns true → bypass.
 *  3. Try to read the cookie; on a populated token, hash + store → "returning visitor" path.
 *  4. No cookie / blank token → mint a new one onto the response → "first visit" path; the store
 *     is NOT touched this request.
 *
 * Any exception escaping the guest logic is **swallowed and logged** before yielding to the
 * downstream chain — visitor tracking is a sidecar concern and must never break the actual
 * request flow. This mirrors soul; deliberately wide catch.
 *
 * Registered via `@Bean` in [io.kudos.ability.web.guest.init.GuestAutoConfiguration]; not
 * declared `@Component` so apps that don't enable the module never instantiate the filter.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestAccessFilter(
    private val service: IGuestAccessService,
    private val store: IGuestAccessStore,
) : OncePerRequestFilter() {

    private val log = LogFactory.getLog(this::class)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!service.isEnabled()) {
            filterChain.doFilter(request, response)
            return
        }
        if (service.isExclude(request)) {
            filterChain.doFilter(request, response)
            return
        }
        doGuestCheck(request, response)
        filterChain.doFilter(request, response)
    }

    private fun doGuestCheck(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            // A fetched GuestAccess with a blank/null token is treated the same as "no cookie"
            // — i.e. the first-visit branch — so a tampered-but-decodable cookie can't sneak
            // past as a returning visitor.
            val returningVisitor = service.fetchGuestToken(request)?.takeUnless { it.token.isNullOrBlank() }
            if (returningVisitor != null) {
                service.hash(request, returningVisitor)
                store.store(returningVisitor)
            } else {
                service.genToken(request, response)
            }
        } catch (e: Exception) {
            // Visitor tracking is a sidecar concern — any failure (store outage, redis timeout,
            // unexpected service bug) must be swallowed so the actual business request still
            // completes. The filter logs and yields to the chain.
            log.error(e, "Guest access filter swallowed an unexpected error")
        }
    }
}
