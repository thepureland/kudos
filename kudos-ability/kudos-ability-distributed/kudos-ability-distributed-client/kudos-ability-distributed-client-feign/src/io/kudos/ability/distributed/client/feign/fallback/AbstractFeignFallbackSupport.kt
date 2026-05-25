package io.kudos.ability.distributed.client.feign.fallback

import feign.FeignException
import io.kudos.base.logger.LogFactory

/**
 * 所有 Feign Fallback 实现共用的降级日志助手。
 *
 * 设计取舍：
 * - 只读接口：记 warn，返回安全默认值（null / 空集合 / 空 map / false），允许调用方继续工作但拿不到最新数据。
 * - 写接口：记 error，并返回 0 / false 等显式失败值，由调用方根据返回值判断是否需要重试或告警。
 * - **不**在 Fallback 中再抛异常，否则会绕过 Feign 容错机制，与提供 Fallback 的初衷相悖。
 *
 * 各 Fallback 类需将 `componentName` 设为自身简单名（如 `"UserAccountFallback"`），便于在日志中识别来源。
 *
 * **关于 4xx / 5xx 区分**：当从 `FallbackFactory.create(cause)` 拿到原始异常时，把异常传给
 * [warnRead] / [errorWrite] 的带 `cause` 重载，日志里会显式打出 `client-error-4xx` /
 * `server-error-5xx` / `unreachable`（无响应）/ `exception-Foo`（非 Feign 异常）等分类，
 * 便于运维分辨"参数错"还是"对端挂"。不带 `cause` 的旧重载仍保留——纯 fallback bean 不挂
 * factory 时按 "unknown" 记录。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
abstract class AbstractFeignFallbackSupport(private val componentName: String) {

    @Suppress("PropertyName")
    protected val log = LogFactory.getLog(this::class)

    /** 记录只读接口降级；不抛出。 */
    protected fun warnRead(method: String, vararg args: Any?) {
        logRead(method, null, args)
    }

    /** 记录只读接口降级，附带触发降级的远程异常（用于区分 4xx / 5xx / unreachable）。 */
    protected fun warnRead(method: String, cause: Throwable?, vararg args: Any?) {
        logRead(method, cause, args)
    }

    /** 记录写接口降级；返回值由调用方判断成败，调用方应据此决定是否补偿。 */
    protected fun errorWrite(method: String, vararg args: Any?) {
        logWrite(method, null, args)
    }

    /** 记录写接口降级，附带触发降级的远程异常。 */
    protected fun errorWrite(method: String, cause: Throwable?, vararg args: Any?) {
        logWrite(method, cause, args)
    }

    private fun logRead(method: String, cause: Throwable?, args: Array<out Any?>) {
        log.warn(
            "[{0}] read-only Feign call degraded ({1}): {2}({3}); 返回安全默认值",
            componentName, describeStatus(cause), method, args.joinToString(),
        )
    }

    private fun logWrite(method: String, cause: Throwable?, args: Array<out Any?>) {
        log.error(
            "[{0}] write Feign call FAILED via fallback ({1}): {2}({3}); 返回失败默认值",
            componentName, describeStatus(cause), method, args.joinToString(),
        )
    }

    /**
     * 把触发降级的异常归类成一段简短字符串。
     *
     * `FeignException.status()` 在远端无响应（连接拒绝 / 超时 / DNS 失败）时返回 0；
     * 有响应时返回 HTTP status code。非 `FeignException`（如 LoadBalancer 拒绝 / 自定义
     * decoder 抛错）按异常类名记录。
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
