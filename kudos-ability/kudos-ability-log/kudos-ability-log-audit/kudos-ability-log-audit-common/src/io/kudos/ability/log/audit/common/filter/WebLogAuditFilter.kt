package io.kudos.ability.log.audit.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

/**
 * Web 审计请求体缓存过滤器。
 *
 * 用 [ContentCachingRequestWrapper] 包裹原始请求，让下游切面（[io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect]）
 * 在请求体已被业务读完后仍能反复读取——HttpServletRequest 的 InputStream 默认只能读一次。
 *
 * 注：使用 Spring Security 的项目要把本 filter 注册进 security 过滤器链，否则 request 会被 security 重写，
 * 本 filter 包裹的对象会被丢弃，请求体缓存能力失效。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class WebLogAuditFilter : OncePerRequestFilter() {

    /**
     * 将原始 request 包成 [ContentCachingRequestWrapper] 后委托给下一个 filter。
     * cache size 传 0 表示按需扩容，避免初始化时分配大缓冲区。
     *
     * @param request 原始请求
     * @param response 响应
     * @param filterChain 过滤器链
     * @author K
     * @since 1.0.0
     */
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
        /** Spring bean 名，方便在 SecurityFilterChain 中按名引用 */
        const val BEAN_NAME: String = "webLogAuditFilter"
    }

}
