package io.kudos.ability.distributed.client.http.client

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport

/**
 * Fallback for [IUnreachableClient], wired via `@HttpServiceFallback` on
 * [io.kudos.ability.distributed.client.http.UnreachableClientConfiguration].
 *
 * Demonstrates the replacement for `@FeignClient(fallback = ...)`. Two constraints Spring Cloud puts
 * on such a class, both of which bite in Kotlin:
 * 1. It is instantiated through its **no-arg constructor**, so no constructor injection.
 * 2. It is wrapped in a **CGLIB proxy**, so the class must be `open` — a plain Kotlin `class` is
 *    final and fails at startup with
 *    `AopConfigException: Could not generate CGLIB subclass ... final class or a non-visible class`.
 *    The `kotlin-spring` allopen plugin does not help here: it only opens classes carrying
 *    `@Component` / `@Service` / etc., and a fallback bound by `@HttpServiceFallback` carries none.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class UnreachableClientFallback :
    IUnreachableClient,
    AbstractHttpFallbackSupport("UnreachableClientFallback") {

    override fun read(): String? {
        warnRead("read")
        return FALLBACK_VALUE
    }

    companion object {
        const val FALLBACK_VALUE: String = "fallback"
    }
}
