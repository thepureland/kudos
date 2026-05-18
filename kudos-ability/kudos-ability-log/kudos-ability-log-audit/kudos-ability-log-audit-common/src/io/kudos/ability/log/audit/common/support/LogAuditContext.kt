package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.LogVo

/**
 * 审计日志上下文。
 *
 * 使用 [InheritableThreadLocal] 存储；但 `childValue` 返回 null —— 子线程**不**继承父线程的
 * 审计上下文，避免线程池里的子任务串台到上一个请求的审计记录。
 *
 * **线程池场景必须在请求/任务结束的 finally 里调 [clear]**，否则下次复用线程时
 * 会读到陈旧的 LogVo。
 *
 * @author K
 * @since 1.0.0
 */
class LogAuditContext {

    private val contextParam: ThreadLocal<LogVo> = object : InheritableThreadLocal<LogVo>() {
        override fun childValue(logVo: LogVo?): LogVo? {
            // 返回 null 表示子线程不继承父线程的值
            return null
        }
    }

    companion object {
        private val self = LogAuditContext()

        /**
         * 设置当前线程的审计日志上下文。
         *
         * @param logVo 日志值对象
         * @author K
         * @since 1.0.0
         */
        fun set(logVo: LogVo?) {
            self.contextParam.set(logVo)
        }

        /**
         * 获取当前线程的审计日志上下文，**没有时自动创建并塞回 ThreadLocal**。
         *
         * 历史 API 语义：[io.kudos.ability.log.audit.common.annotation.LogAuditAspect.after]
         * 假设 `set` 之后 `get` 一定非空；但某些路径只走 `after` 而没走 `before`，所以这里
         * 兜底创建。副作用：
         *  - **只读侦测**（"当前线程有 LogVo 吗？"）会污染 ThreadLocal，留下空 [LogVo] 实例。
         *  - 线程池复用线程时这个空 LogVo 不会被自动清，下一个任务读出来误以为已有审计上下文。
         *
         * 纯只读检查请改用 [getOrNull]；调用方仍需在请求 / 任务结束的 finally 里调 [clear]。
         *
         * @return 当前线程的 LogVo（可能是刚创建的空实例）
         * @author K
         * @since 1.0.0
         */
        fun get(): LogVo? {
            if (self.contextParam.get() == null) {
                self.contextParam.set(LogVo())
            }
            return self.contextParam.get()
        }

        /**
         * 只读侦测：返回当前线程的 LogVo，**没有时返回 null 而不创建**。
         * 用于"看看有没有"而不是"我要用"的场景，避免污染 ThreadLocal。
         */
        fun getOrNull(): LogVo? = self.contextParam.get()

        /**
         * 清除当前线程的审计日志上下文。
         *
         * 建议在请求 / 任务结束时调用此方法，避免线程池复用线程时造成上下文污染和内存泄漏。
         *
         * @author K
         * @since 1.0.0
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
