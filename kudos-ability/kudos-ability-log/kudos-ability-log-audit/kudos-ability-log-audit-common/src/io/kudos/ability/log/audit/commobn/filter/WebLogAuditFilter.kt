package io.kudos.ability.log.audit.commobn.filter

import jakarta.servlet.FilterChain

/**
 * 使用spring-security，同时需要WebLogAudit，需要将此filter注册到security过滤器链中
 */
class WebLogAuditFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    protected override fun doFilterInternal(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        filterChain: FilterChain
    ) {
        //使用spring-security时，request会被重写
        val wrapper: ContentCachingRequestWrapper = ContentCachingRequestWrapper(request as HttpServletRequest?)
        filterChain.doFilter(wrapper, response)
    }

    companion object {
        const val BEAN_NAME: String = "webLogAuditFilter"
    }
}
