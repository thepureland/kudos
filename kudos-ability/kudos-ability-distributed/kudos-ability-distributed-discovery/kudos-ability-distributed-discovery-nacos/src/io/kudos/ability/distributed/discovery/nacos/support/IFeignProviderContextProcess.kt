package io.kudos.ability.distributed.discovery.nacos.support

import io.kudos.context.core.KudosContext
import jakarta.servlet.http.HttpServletRequest

/**
 * Provider-side Feign context extension SPI.
 *
 * Dual of the client-side `IFeignRequestContextProcess` (in the `kudos-ability-distributed-client-feign`
 * module) — after the client writes headers, the provider can use this SPI to parse those headers
 * (which are not [KudosContext] fields) back into ThreadLocal / other global state.
 *
 * Typical example: Seata global transaction XID
 *  - Client-side `SeataFeignXidProcessor` writes `RootContext.getXID()` into the `TX_XID` header
 *  - Provider side may implement this SPI to bind `request.getHeader("TX_XID")` back via `RootContext.bind(xid)`
 *    (in production this goes through `SeataXidServletFilter`, but routing it via this SPI is also valid)
 *
 * Application code just registers the implementation as a Spring bean — [io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter]
 * will discover and invoke every implementation automatically.
 *
 * @author K
 * @since 1.0.0
 */
interface IFeignProviderContextProcess {
    /**
     * Read business-defined propagation headers from the HTTP request and write them back into [KudosContext] or other ThreadLocal state.
     *
     * @param request the current HTTP request (guaranteed non-null)
     * @param context the current thread's KudosContext (readable and writable)
     */
    fun processContext(request: HttpServletRequest, context: KudosContext)
}
