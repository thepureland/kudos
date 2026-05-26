package io.kudos.ability.data.rdb.jdbc.context

/**
 * Thread-local context for database routing parameters.
 *
 * Uses [InheritableThreadLocal] to store the current thread's [DbParam], which AOP
 * aspects use to select the data source. Child threads **do not** inherit the parent
 * thread's value (`childValue` returns null) — preventing tasks in a thread pool
 * from accidentally inheriting the routing intent of a previous request.
 *
 * Same contract as `KudosContextHolder`: when using thread pools you **must** call
 * [clear] at the end of each request/task, otherwise a reused thread will carry the
 * old [DbParam] into the next request, causing data-source crossover.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DbContext {
    private val contextParam: ThreadLocal<DbParam?> = object : InheritableThreadLocal<DbParam?>() {
        override fun childValue(parentValue: DbParam?): DbParam? {
            // Child threads do not inherit the parent's value; this prevents "the previous task's routing intent being inherited by a new task" in thread pools.
            return null
        }
    }

    companion object {
        private val self = DbContext()

        /**
         * Explicitly writes the current thread's [DbParam]. Passing `null` is
         * equivalent to clearing the current thread's binding (but **does not**
         * remove the ThreadLocal slot, only nulls it — use [clear] to truly remove).
         */
        fun set(param: DbParam?) {
            self.contextParam.set(param)
        }

        /**
         * Returns the current thread's [DbParam]; **if not bound, automatically
         * creates an empty [DbParam] and writes it back into the ThreadLocal**.
         *
         * This "create-on-read" semantic is historical — the aspects require get()
         * to always return non-null so callers can chain `DbContext.get().forcedDs = ...`.
         * The cost is that calling get() in an unaware code path silently inserts an
         * empty DbParam into the current thread's ThreadLocal slot, which becomes a
         * leak source in thread-pool scenarios if [clear] is forgotten.
         *
         * **If you only want to "peek at whether there is currently a context", use [getOrNull].**
         */
        fun get(): DbParam {
            if (self.contextParam.get() == null) {
                self.contextParam.set(DbParam())
            }
            return requireNotNull(self.contextParam.get()) { "contextParam is null" }
        }

        /**
         * Returns the current thread's bound [DbParam]; returns `null` when unbound,
         * **without creating any new object** and **without writing to the ThreadLocal**.
         * Used by "read-only inspection" paths to avoid being polluted by [get]'s side effects.
         */
        fun getOrNull(): DbParam? = self.contextParam.get()

        /**
         * Completely clears the current thread's [DbParam] binding (`ThreadLocal.remove`).
         * **Must be called at the end of every task in thread-pool scenarios**, otherwise
         * a reused thread will carry the old [DbParam] into the next task.
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
