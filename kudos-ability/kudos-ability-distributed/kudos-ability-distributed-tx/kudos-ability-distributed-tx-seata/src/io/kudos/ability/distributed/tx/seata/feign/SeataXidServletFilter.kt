package io.kudos.ability.distributed.tx.seata.feign

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.seata.core.context.RootContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 服务端入口的 Seata XID 还原过滤器。
 *
 * 配对的客户端逻辑见 [SeataFeignXidProcessor]：客户端把 `RootContext.getXID()`
 * 写到请求头 `TX_XID`；这里在每次请求开始时读出它并 `bind` 回当前线程的
 * [RootContext]，让链路下游的 `@Transactional` + ConnectionProxy 能识别正在
 * 进行的全局事务并注册自己的 AT 分支。请求结束时必须 `unbind` 避免线程池
 * 里的污染。
 *
 * 用 `Ordered.HIGHEST_PRECEDENCE` 让这个过滤器尽量早跑：必须在任何业务
 * `@Transactional` 切面之前完成 bind，否则切面里取到的 XID 还是 null。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class SeataXidServletFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val xid = request.getHeader(RootContext.KEY_XID)
        val bound = !xid.isNullOrBlank() && RootContext.getXID() == null
        if (bound) RootContext.bind(xid)
        try {
            filterChain.doFilter(request, response)
        } finally {
            if (bound) RootContext.unbind()
        }
    }
}
