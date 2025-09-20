package io.kudos.ability.log.audit.commobn.support

import io.kudos.ability.log.audit.commobn.entity.BaseLog
import io.kudos.ability.log.audit.commobn.entity.LogVo


class LogAuditContext {
    private val contextParam: ThreadLocal<LogVo?> = object : InheritableThreadLocal<LogVo?>() {
        override fun childValue(logVo: LogVo?): LogVo? {
            // 返回 null 表示子线程不继承父线程的值
            return null
        }
    }

    companion object {
        private val self = LogAuditContext()

        fun set(logVo: LogVo?) {
            self.contextParam.set(logVo)
        }

        fun get(): LogVo? {
            if (self.contextParam.get() == null) {
                self.contextParam.set(LogVo())
            }
            return self.contextParam.get()
        }

        val auditLog: BaseLog?
            get() {
                if (self.contextParam.get() == null) {
                    self.contextParam.set(LogVo())
                }
                return self.contextParam.get()?.logs
            }
    }
}
