package io.kudos.ability.log.audit.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

/**
 * 使用spring-security，同时需要WebLogAudit，需要将此filter注册到security过滤器链中
 */
class WebLogAuditFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        //使用spring-security时，request会被重写
        val wrapper = ContentCachingRequestWrapper(request, 0)
        filterChain.doFilter(wrapper, response)
    }

    companion object {
        const val BEAN_NAME: String = "webLogAuditFilter"
    }

}
