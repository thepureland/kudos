package io.kudos.ability.distributed.tx.seata.feign

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.seata.core.context.RootContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Server-side entry filter that restores the Seata XID.
 *
 * For the paired client-side logic see [SeataFeignXidProcessor]: the client writes
 * `RootContext.getXID()` into the `TX_XID` request header; here we read it at the start of every
 * request and `bind` it back into the current thread's [RootContext], so that downstream
 * `@Transactional` + ConnectionProxy can recognise the in-flight global transaction and register
 * their own AT branches. We must `unbind` at the end of the request to avoid polluting the thread
 * pool.
 *
 * `Ordered.HIGHEST_PRECEDENCE` makes this filter run as early as possible: bind must complete
 * before any business `@Transactional` aspect, otherwise the XID seen inside the aspect is still null.
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
