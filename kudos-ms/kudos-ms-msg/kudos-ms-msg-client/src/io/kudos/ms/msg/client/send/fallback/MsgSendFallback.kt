package io.kudos.ms.msg.client.send.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.msg.client.send.proxy.IMsgSendProxy
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest


/**
 * Fallback implementation for message send.
 *
 * publish is a write API; when the remote is unreachable, an error is logged and null is returned,
 * so the caller can queue a retry or apply business-side compensation.
 *
 * **Why every method appears twice.** Feign supplied the triggering exception through
 * `FallbackFactory<T>.create(cause)`, which let the log distinguish 4xx / 5xx / unreachable.
 * `@HttpServiceFallback` instantiates the fallback through its no-arg constructor instead, but
 * `CircuitBreakerConfigurerUtils.resolveFallbackMethod` looks the method up **twice** — first as
 * `name(Throwable, ...args)`, then as `name(...args)` — so the `Throwable`-first overload is what
 * actually runs at fallback time, and it receives the cause.
 *
 * The plain `override` is kept solely so the compiler still enforces that every contract method has
 * a fallback; dropping it would make the class stop implementing the proxy interface and a newly
 * added contract method would silently have no fallback. It only executes if someone calls the
 * fallback directly (as the unit tests do).
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class MsgSendFallback : AbstractHttpFallbackSupport("MsgSendFallback"), IMsgSendProxy {

    fun publish(cause: Throwable?, request: MsgPublishRequest): String? {
        errorWrite("publish", cause, request)
        return null
    }

    override fun publish(request: MsgPublishRequest): String? = publish(null, request)

}
