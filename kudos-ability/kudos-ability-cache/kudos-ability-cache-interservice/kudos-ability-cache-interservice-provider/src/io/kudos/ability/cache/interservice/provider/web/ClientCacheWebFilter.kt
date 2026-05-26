package io.kudos.ability.cache.interservice.provider.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.kudos.ability.cache.interservice.common.ClientCacheKey

/**
 * Client cache web filter.
 * Wraps the HTTP request as a CacheClientRequest to support request handling for inter-service caching.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class ClientCacheWebFilter(
    private val wrapAllRequests: Boolean = false
) : Filter {

    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain
    ) {
        val request = servletRequest as? HttpServletRequest
        val response = servletResponse as? HttpServletResponse
        if (request == null || response == null || !shouldWrap(request)) {
            filterChain.doFilter(servletRequest, servletResponse)
            return
        }
        filterChain.doFilter(CacheClientRequest(request, response), servletResponse)
    }

    private fun shouldWrap(request: HttpServletRequest): Boolean {
        return wrapAllRequests ||
            request.getHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY) != null ||
            request.getHeader(ClientCacheKey.HEADER_KEY_CACHE_UID) != null
    }
}
