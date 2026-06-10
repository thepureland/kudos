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
     *
     * The constructor's `int` parameter is a **contentCacheLimit** (maximum bytes cached per request),
     * *not* an initial buffer size — the historical `ContentCachingRequestWrapper(request, 0)` call
     * meant "cache at most 0 bytes", so the body replay in `AuditLogTool.getRequestData` always saw
     * an empty body and web audits silently lost their request payloads. Now caches up to
     * [CONTENT_CACHE_LIMIT_BYTES]; the buffer itself still grows on demand, so small requests do not
     * pre-allocate the full limit.
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
        val wrapper = ContentCachingRequestWrapper(request, CONTENT_CACHE_LIMIT_BYTES)
        filterChain.doFilter(wrapper, response)
    }

    companion object {
        /** Spring bean name, for referencing by name in a SecurityFilterChain */
        const val BEAN_NAME: String = "webLogAuditFilter"

        /**
         * Max request-body bytes cached for audit replay. Large enough for any realistic audited
         * payload, while bounding the per-request memory cost of caching (an unbounded cache would
         * let one oversized upload balloon the heap).
         */
        const val CONTENT_CACHE_LIMIT_BYTES: Int = 10 * 1024 * 1024
    }

}
