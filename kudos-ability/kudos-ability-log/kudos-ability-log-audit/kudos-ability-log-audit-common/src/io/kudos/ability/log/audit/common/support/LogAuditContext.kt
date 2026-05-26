package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.LogVo

/**
 * Audit log context.
 *
 * Stored in an [InheritableThreadLocal]; however, `childValue` returns null —
 * child threads **do not** inherit the parent thread's audit context, preventing
 * tasks in a thread pool from leaking the previous request's audit record.
 *
 * **In thread-pool scenarios, [clear] must be called in a finally block at the
 * end of the request/task**, otherwise the reused thread will read a stale LogVo.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LogAuditContext {

    private val contextParam: ThreadLocal<LogVo> = object : InheritableThreadLocal<LogVo>() {
        override fun childValue(logVo: LogVo?): LogVo? {
            // Returning null means the child thread does not inherit the parent's value.
            return null
        }
    }

    companion object {
        private val self = LogAuditContext()

        /**
         * Sets the audit log context for the current thread.
         *
         * @param logVo log value object
         * @author K
         * @since 1.0.0
         */
        fun set(logVo: LogVo?) {
            self.contextParam.set(logVo)
        }

        /**
         * Returns the audit log context for the current thread; **if none exists,
         * automatically creates one and stores it back into the ThreadLocal**.
         *
         * Historical API semantics:
         * [io.kudos.ability.log.audit.common.annotation.LogAuditAspect.after]
         * assumes that after `set`, `get` is always non-null; but some paths only
         * go through `after` without `before`, so this falls back to creating one.
         * Side effects:
         *  - **Read-only probes** ("does the current thread have a LogVo?") will
         *    pollute the ThreadLocal with an empty [LogVo] instance.
         *  - When a thread pool reuses the thread, the empty LogVo is not auto-cleared,
         *    so the next task reads it and mistakenly believes the audit context exists.
         *
         * Use [getOrNull] for pure read-only checks; callers must still call [clear]
         * in a finally block at the end of the request / task.
         *
         * @return the current thread's LogVo (possibly a freshly created empty instance)
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
         * Read-only probe: returns the current thread's LogVo; **returns null
         * without creating one when absent**. For "check if present" rather than
         * "I want to use it" scenarios, to avoid polluting the ThreadLocal.
         */
        fun getOrNull(): LogVo? = self.contextParam.get()

        /**
         * Clears the current thread's audit log context.
         *
         * Recommended to call this at the end of the request / task to avoid
         * context pollution and memory leaks when a thread pool reuses threads.
         *
         * @author K
         * @since 1.0.0
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
