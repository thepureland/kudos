package io.kudos.ability.distributed.client.feign.fallback

import feign.FeignException
import io.kudos.base.logger.LogFactory

/**
 * Shared logging helper for all Feign fallback implementations.
 *
 * Design choices:
 * - Read-only endpoints: log at warn level and return safe defaults (null / empty collection /
 *   empty map / false) so callers can keep working but won't get fresh data.
 * - Write endpoints: log at error level and return explicit failure values (0 / false) so callers
 *   can decide on retry or alerting based on the return value.
 * - **Never** throw from a fallback — that bypasses Feign's resilience mechanism and defeats the
 *   purpose of providing one.
 *
 * Each fallback class must set `componentName` to its simple class name (e.g. `"UserAccountFallback"`)
 * so the log clearly identifies the source.
 *
 * **About distinguishing 4xx / 5xx**: when the upstream exception is available via
 * `FallbackFactory.create(cause)`, pass it to the `cause`-taking overloads of [warnRead] /
 * [errorWrite]. The log then categorizes as `client-error-4xx` / `server-error-5xx` /
 * `unreachable` (no response) / `exception-Foo` (non-Feign exception), helping operators tell
 * "bad params" from "remote down". The cause-less overloads remain — plain fallback beans without
 * a factory are logged as "unknown".
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
abstract class AbstractFeignFallbackSupport(private val componentName: String) {

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
            "[{0}] read-only Feign call degraded ({1}): {2}({3}); returning safe default",
            componentName, describeStatus(cause), method, args.joinToString(),
        )
    }

    private fun logWrite(method: String, cause: Throwable?, args: Array<out Any?>) {
        log.error(
            "[{0}] write Feign call FAILED via fallback ({1}): {2}({3}); returning failure default",
            componentName, describeStatus(cause), method, args.joinToString(),
        )
    }

    /**
     * Categorizes the triggering exception into a short string.
     *
     * `FeignException.status()` returns 0 when the remote does not respond (connection refused /
     * timeout / DNS failure) and the HTTP status code otherwise. Non-`FeignException` causes
     * (e.g. LoadBalancer rejection, custom decoder throws) are logged by exception class name.
     */
    private fun describeStatus(cause: Throwable?): String = when {
        cause == null -> "unknown"
        cause is FeignException -> when (val s = cause.status()) {
            0 -> "unreachable"
            in 400..499 -> "client-error-$s"
            in 500..599 -> "server-error-$s"
            else -> "status-$s"
        }
        else -> "exception-${cause.javaClass.simpleName}"
    }
}
