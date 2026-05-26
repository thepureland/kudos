package io.kudos.ability.distributed.client.feign.support

import feign.RequestTemplate
import io.kudos.context.core.KudosContext

/**
 * Feign request-header extension SPI.
 *
 * `GlobalHeaderRequestInterceptor` automatically writes standard fields from [KudosContext]
 * (tenantId / subSysCode / traceKey, etc.) into request headers; **state that must be forwarded
 * across services but does not live in KudosContext** (canonical example: the Seata global
 * transaction XID lives in `RootContext`, not `KudosContext`) is injected via this SPI.
 *
 * Current production implementation: `SeataFeignXidProcessor` (in the
 * `kudos-ability-distributed-tx-seata` module) writes `RootContext.getXID()` to the `TX_XID`
 * header; the server-side `SeataXidServletFilter` binds it back into the current thread's
 * Seata context.
 *
 * It becomes effective simply by registering as a Spring bean —
 * `GlobalHeaderRequestInterceptor` discovers all implementations via
 * `SpringKit.getBeansOfType<IFeignRequestContextProcess>()`.
 *
 * @author K
 * @since 1.0.0
 */
interface IFeignRequestContextProcess {
    /**
     * Feign request-header extension point.
     *
     * @param requestTemplate the Feign request template; call `.header(...)` to write headers that
     *        must be propagated
     * @param context the current Kudos context (it may not actually contain the state to forward;
     *        the business side can read it from ThreadLocal, `RootContext`, or other sources)
     */
    fun processContext(requestTemplate: RequestTemplate, context: KudosContext)
}
