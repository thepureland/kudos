package io.kudos.ability.cache.interservice.provider.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.kudos.ability.cache.interservice.common.ClientCacheKey

/**
 * 客户端缓存Web过滤器
 * 包装HTTP请求为CacheClientRequest，支持服务间缓存的请求处理
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
