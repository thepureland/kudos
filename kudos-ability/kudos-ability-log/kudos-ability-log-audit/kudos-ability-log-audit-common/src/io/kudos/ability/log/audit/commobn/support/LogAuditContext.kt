package io.kudos.ability.log.audit.commobn.support

import io.kudos.ability.log.audit.commobn.entity.BaseLog
import io.kudos.ability.log.audit.commobn.entity.LogVo

/**
 * 审计日志上下文
 * 
 * 使用 InheritableThreadLocal 存储线程上下文，支持父子线程间传递。
 * 注意：在使用线程池的场景下，必须在请求/任务结束后调用 clear() 方法清理上下文，
 * 避免内存泄漏。
 *
 * @author K
 * @since 1.0.0
 */
class LogAuditContext {
    private val contextParam: ThreadLocal<LogVo?> = object : InheritableThreadLocal<LogVo?>() {
        override fun childValue(logVo: LogVo?): LogVo? {
            // 返回 null 表示子线程不继承父线程的值
            return null
        }
    }

    companion object {
        private val self = LogAuditContext()

        /**
         * 设置当前线程的审计日志上下文
         *
         * @param logVo 日志值对象
         * @author K
         * @since 1.0.0
         */
        fun set(logVo: LogVo?) {
            self.contextParam.set(logVo)
        }

        /**
         * 获取当前线程的审计日志上下文
         * 如果当前线程没有上下文，会创建一个新的并设置到 ThreadLocal 中
         *
         * @return 日志值对象
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
         * 获取当前线程的审计日志
         *
         * @return 审计日志对象
         * @author K
         * @since 1.0.0
         */
        val auditLog: BaseLog?
            get() {
                if (self.contextParam.get() == null) {
                    self.contextParam.set(LogVo())
                }
                return self.contextParam.get()?.logs
            }

        /**
         * 清除当前线程的审计日志上下文
         * 
         * 建议在请求/任务结束时调用此方法，避免线程池复用线程时造成上下文污染和内存泄漏。
         * 
         * @author K
         * @since 1.0.0
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
