package io.kudos.ms.sys.client.support

import io.kudos.base.logger.LogFactory

/**
 * sys 模块所有 Feign Fallback 共用的降级日志辅助。
 *
 * 设计取舍：
 * - 只读接口：记 warn，返回安全默认值（null / 空集合 / 空 map / false），允许调用方继续工作但拿不到最新数据。
 * - 写接口：记 error，并返回 0 / false 等显式失败值，由调用方根据返回值判断是否需要重试或告警。
 * - **不**在 Fallback 中再抛异常，否则会绕过 Feign 容错机制，与提供 Fallback 的初衷相悖。
 *
 * 各 Fallback 类需将 `componentName` 设为自身简单名（如 `"SysDictFallback"`），便于在日志中识别来源。
 */
abstract class SysClientFallbackSupport(private val componentName: String) {

    @Suppress("PropertyName")
    protected val log = LogFactory.getLog(this::class)

    /** 记录只读接口降级；不抛出。 */
    protected fun warnRead(method: String, vararg args: Any?) {
        log.warn(
            "[{0}] read-only Feign call degraded: {1}({2}); 返回安全默认值",
            componentName, method, args.joinToString(),
        )
    }

    /** 记录写接口降级；返回值由调用方判断成败，调用方应据此决定是否补偿。 */
    protected fun errorWrite(method: String, vararg args: Any?) {
        log.error(
            "[{0}] write Feign call FAILED via fallback: {1}({2}); 返回失败默认值",
            componentName, method, args.joinToString(),
        )
    }
}
