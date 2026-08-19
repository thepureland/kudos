package io.kudos.ability.distributed.client.http.support

import io.kudos.context.core.KudosContext
import org.springframework.http.HttpRequest

/**
 * Interface-client request-header extension SPI.
 *
 * The interface-client counterpart of `IFeignRequestContextProcess`: `KudosContextRequestInterceptor`
 * writes the standard [KudosContext] fields (tenantId / subSysCode / traceKey, etc.) into request
 * headers, and **state that must be forwarded across services but does not live in KudosContext**
 * (canonical example: the Seata global transaction XID lives in `RootContext`) is injected here.
 *
 * Only the first parameter differs from the Feign SPI — Feign's `RequestTemplate` becomes Spring's
 * [HttpRequest], whose `headers` is a plain `HttpHeaders`:
 *
 * ```kotlin
 * @Component
 * class SeataXidProcessor : IHttpRequestContextProcess {
 *     override fun processContext(request: HttpRequest, context: KudosContext) {
 *         RootContext.getXID()?.let { request.headers.add("TX_XID", it) }
 *     }
 * }
 * ```
 *
 * Implementations take effect simply by being registered as Spring beans, and are invoked in
 * `Ordered` / `@Order` order.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IHttpRequestContextProcess {

    /**
     * Interface-client request-header extension point.
     *
     * @param request the outbound request; call `request.headers.add(...)` to write headers that must
     *        be propagated. Mutating headers here is safe — [org.springframework.http.client.ClientHttpRequestInterceptor]
     *        runs before the request is committed.
     * @param context the current Kudos context (it may not actually contain the state to forward;
     *        the business side can read it from ThreadLocal, `RootContext`, or other sources)
     */
    fun processContext(request: HttpRequest, context: KudosContext)
}
