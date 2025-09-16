package io.kudos.ability.cache.interservice.provider.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class ClientCacheWebFilter : Filter {

    public override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain
    ) {
        val feinRequest: HttpServletRequest =
            CacheClientRequest(servletRequest as HttpServletRequest?, servletResponse as HttpServletResponse?)
        filterChain.doFilter(feinRequest, servletResponse)
    }
}
