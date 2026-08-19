package io.kudos.ability.distributed.client.http.client

/**
 * Fallback that receives the triggering exception.
 *
 * `CircuitBreakerConfigurerUtils.resolveFallbackMethod` looks the fallback method up **twice**: first
 * as `name(Throwable, ...originalArgs)`, then as `name(...originalArgs)`. Declaring the
 * `Throwable`-first overload is therefore the interface-client replacement for Feign's
 * `FallbackFactory<T>.create(cause)` — the capability is not lost, only reshaped.
 *
 * Note this class does **not** implement [ICauseAwareClient]: with a `Throwable` first parameter the
 * signatures no longer match the contract, and Spring Cloud resolves fallback methods reflectively by
 * name rather than through the interface. That is the trade-off — cause-awareness costs the
 * compile-time guarantee that every contract method has a fallback.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class CauseAwareFallback {

    fun read(cause: Throwable, key: String): String? = "$key:${cause.javaClass.simpleName}"

}
