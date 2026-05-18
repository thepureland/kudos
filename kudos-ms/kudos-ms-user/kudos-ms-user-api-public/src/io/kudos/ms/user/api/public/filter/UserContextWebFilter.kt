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
 * 把 `HttpSession[SESSION_KEY_USER]` 里的 [SessionUserPrincipal] 灌进 [KudosContext.user]。
 *
 * 必须排在 `io.kudos.ability.web.springmvc.filter.WebContextInitFilter` **之后**——后者负责
 * 创建 [KudosContext] 实例并写入 ThreadLocal；本类只补 `user` 字段。
 * 顺序通过 [Order]：`WebContextInitFilter` 默认是 Spring 自动配置的非主优先级（≥0），
 * 这里取 `Ordered.LOWEST_PRECEDENCE - 100` 一个相对靠后的值。
 *
 * **不会自动 invalidate session**：登出策略由 `PassportPublicController.logout` 显式触发；
 * 本过滤器只是单向"读 session → 写 context"。
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
        // session=false：不要为本次请求强行创建空会话；只有已登录过的会有 session
        val session = request.getSession(false)
        val raw = session?.getAttribute(KudosContext.SESSION_KEY_USER)
        if (raw is SessionUserPrincipal) {
            // KudosContextHolder.get() 自动创建空 context 写入 ThreadLocal（若 WebContextInitFilter
            // 未先跑也兼容）；我们只补 user 字段。
            KudosContextHolder.get().user = raw
        }
        filterChain.doFilter(request, response)
    }
}
