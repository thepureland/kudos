package io.kudos.ability.distributed.client.http.init

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Supplies the one Resilience4j default this module cannot safely leave at its framework value.
 *
 * ## Why
 *
 * `spring-cloud-starter-circuitbreaker-resilience4j` is an `api` dependency of this module (it is what
 * makes `@HttpServiceFallback` work at all), and out of the box it wraps every call in a **TimeLimiter
 * backed by a thread pool**. That pool is the problem: [io.kudos.context.core.KudosContextHolder] is a
 * ThreadLocal, so the outbound call executes on a pool thread where the caller's `KudosContext` does
 * not exist. `KudosContextRequestInterceptor` then reads a blank context and the request goes out with
 * **no tenant id, no subsystem code and a fresh trace key** — silently. In a multi-tenant system that
 * is a correctness and isolation failure, not a performance note.
 *
 * Setting `disable-thread-pool = true` runs the call on the caller's thread, which restores context
 * propagation while keeping the circuit breaker and `@HttpServiceFallback` fully functional (both are
 * covered by `HttpServiceClientTest`).
 *
 * ## What this costs, and what replaces it
 *
 * On the caller's thread Resilience4j cannot interrupt a call, so the **TimeLimiter stops enforcing
 * anything** — verified: a 3-second endpoint returns successfully with a 1-second limiter configured.
 * The bound on call duration therefore has to come from the HTTP layer instead, per group:
 *
 * ```yaml
 * spring:
 *   http:
 *     serviceclient:
 *       sys:
 *         base-url: lb://kudos-ms-sys
 *         connect-timeout: 2s
 *         read-timeout: 5s     # ← this is now the real timeout
 * ```
 *
 * A read timeout surfaces as `ResourceAccessException` and routes into the group's fallback exactly
 * like any other failure ([io.kudos.ability.distributed.client.http.fallback.HttpFallbackStatusResolver]
 * maps it to 504).
 *
 * These are **defaults, not overrides**: anything set in the application's own configuration wins,
 * because this property source is appended last. An application that genuinely needs a hard total-call
 * timeout can set `disable-thread-pool: false` again — but it must then propagate the kudos context
 * onto the Resilience4j pool itself, or accept losing it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class HttpClientDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val defaults = mapOf<String, Any>(
            "spring.cloud.circuitbreaker.resilience4j.disable-thread-pool" to true
        )
        // addLast: an explicit setting anywhere else in the environment takes precedence.
        environment.propertySources.addLast(MapPropertySource(PROPERTY_SOURCE_NAME, defaults))
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    companion object {
        const val PROPERTY_SOURCE_NAME: String = "kudosHttpClientDefaults"
    }
}
