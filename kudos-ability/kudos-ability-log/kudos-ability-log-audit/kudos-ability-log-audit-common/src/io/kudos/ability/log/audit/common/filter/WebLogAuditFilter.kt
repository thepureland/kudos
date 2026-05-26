package io.kudos.ability.log.audit.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

/**
 * Web audit request-body caching filter.
 *
 * Wraps the original request with [ContentCachingRequestWrapper] so downstream aspects
 * ([io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect]) can read the request body repeatedly even after
 * the business code has consumed it — `HttpServletRequest`'s InputStream can only be read once by default.
 *
 * Note: projects using Spring Security must register this filter into the security filter chain, otherwise the
 * request gets rewritten by security, the object wrapped by this filter is discarded, and the body-caching capability
 * is lost.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class WebLogAuditFilter : OncePerRequestFilter() {

    /**
     * Wraps the original request as a [ContentCachingRequestWrapper] then delegates to the next filter.
     * A cache size of 0 means grow on demand, avoiding allocating a large buffer at init time.
     *
     * @param request the original request
     * @param response the response
     * @param filterChain the filter chain
     * @author K
     * @since 1.0.0
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // When using spring-security, the request gets rewritten
        val wrapper = ContentCachingRequestWrapper(request, 0)
        filterChain.doFilter(wrapper, response)
    }

    companion object {
        /** Spring bean name, for referencing by name in a SecurityFilterChain */
        const val BEAN_NAME: String = "webLogAuditFilter"
    }

}
