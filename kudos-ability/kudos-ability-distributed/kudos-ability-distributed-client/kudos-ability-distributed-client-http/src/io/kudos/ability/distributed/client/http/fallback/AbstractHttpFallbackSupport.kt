package io.kudos.ability.distributed.client.http.fallback

import io.kudos.base.logger.LogFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException

/**
 * Shared logging helper for all interface-client fallback implementations.
 *
 * Port of `AbstractFeignFallbackSupport`; the logging conventions are unchanged:
 * - Read-only endpoints: log at warn level and return safe defaults (null / empty collection / false)
 *   so callers keep working but won't get fresh data.
 * - Write endpoints: log at error level and return explicit failure values (0 / false) so callers can
 *   decide on retry or compensation.
 * - **Never** throw from a fallback — that defeats the circuit breaker it is attached to.
 *
 * Usage with `@HttpServiceFallback` (the interface-client equivalent of `@FeignClient(fallback=...)`):
 *
 * ```kotlin
 * @Component
 * class UserClientFallback : IUserClient, AbstractHttpFallbackSupport("UserClientFallback") {
 *     override fun getUser(id: Long): User? {
 *         warnRead("getUser", id)
 *         return null
 *     }
 * }
 * ```
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
abstract class AbstractHttpFallbackSupport(private val componentName: String) {

    @Suppress("PropertyName")
    protected val log = LogFactory.getLog(this::class)

    /** Logs a read-only fallback; does not throw. */
    protected fun warnRead(method: String, vararg args: Any?) {
        logRead(method, null, args)
    }

    /** Logs a read-only fallback with the remote exception that triggered it (used to tell 4xx / 5xx / unreachable apart). */
    protected fun warnRead(method: String, cause: Throwable?, vararg args: Any?) {
        logRead(method, cause, args)
    }

    /** Logs a write fallback; the caller decides success/failure from the return value and should compensate accordingly. */
    protected fun errorWrite(method: String, vararg args: Any?) {
        logWrite(method, null, args)
    }

    /** Logs a write fallback with the remote exception that triggered it. */
    protected fun errorWrite(method: String, cause: Throwable?, vararg args: Any?) {
        logWrite(method, cause, args)
    }

    private fun logRead(method: String, cause: Throwable?, args: Array<out Any?>) {
        log.warn(
            "[{0}] read-only remote call degraded ({1}): {2}({3}); returning safe default",
            componentName, describeStatus(cause), method, args.joinToString(),
        )
    }

    private fun logWrite(method: String, cause: Throwable?, args: Array<out Any?>) {
        log.error(
            "[{0}] write remote call FAILED via fallback ({1}): {2}({3}); returning failure default",
            componentName, describeStatus(cause), method, args.joinToString(),
        )
    }

    /**
     * Categorizes the triggering exception into a short string.
     *
     * Unlike Feign — where a single `FeignException.status()` of 0 means "no response" — RestClient
     * splits the two cases into different types: [RestClientResponseException] carries a real status
     * code, while [ResourceAccessException] means the remote never answered (connection refused /
     * timeout / DNS failure). Both are looked up through the cause chain, since the circuit breaker
     * hands the fallback whatever wrapper it caught.
     *
     * Internal (instead of private) so the pure categorization logic can be unit-tested directly.
     */
    internal fun describeStatus(cause: Throwable?): String {
        if (cause == null) return "unknown"
        val chain = HttpFallbackStatusResolver.causeChain(cause)
        val response = chain.filterIsInstance<RestClientResponseException>().firstOrNull()
        if (response != null) {
            return when (val s = response.statusCode.value()) {
                in 400..499 -> "client-error-$s"
                in 500..599 -> "server-error-$s"
                else -> "status-$s"
            }
        }
        if (chain.any { it is ResourceAccessException }) return "unreachable"
        return "exception-${cause.javaClass.simpleName}"
    }
}
