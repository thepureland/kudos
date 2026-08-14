package io.kudos.ability.data.rdb.jdbc.context

/**
 * Thread-local context for database routing parameters.
 *
 * Holds the current thread's [DbParam] in a plain [ThreadLocal], which AOP advices use to select
 * the data source. Child threads never see the parent's value — a new thread starts with an
 * unbound slot, preventing tasks in a thread pool from accidentally inheriting the routing
 * intent of a previous request.
 *
 * Same contract as `KudosContextHolder`: when using thread pools you **must** call
 * [clear] at the end of each request/task, otherwise a reused thread will carry the
 * old [DbParam] into the next request, causing data-source crossover.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object DbContext {

    private val contextParam = ThreadLocal<DbParam?>()

    /**
     * Explicitly writes the current thread's [DbParam]. Passing `null` is
     * equivalent to clearing the current thread's binding (but **does not**
     * remove the ThreadLocal slot, only nulls it — use [clear] to truly remove).
     */
    fun set(param: DbParam?) {
        contextParam.set(param)
    }

    /**
     * Returns the current thread's [DbParam]; **if not bound, automatically
     * creates an empty [DbParam] and writes it back into the ThreadLocal**.
     *
     * This "create-on-read" semantic is historical — legacy callers require get()
     * to always return non-null so they can chain `DbContext.get().forcedDs = ...`.
     * The cost is that calling get() in an unaware code path silently inserts an
     * empty DbParam into the current thread's ThreadLocal slot, which becomes a
     * leak source in thread-pool scenarios if [clear] is forgotten. The framework
     * itself no longer calls it — **new code should use [getOrNull] and [set]**.
     */
    fun get(): DbParam {
        var param = contextParam.get()
        if (param == null) {
            param = DbParam()
            contextParam.set(param)
        }
        return param
    }

    /**
     * Returns the current thread's bound [DbParam]; returns `null` when unbound,
     * **without creating any new object** and **without writing to the ThreadLocal**.
     * Used by "read-only inspection" paths to avoid being polluted by [get]'s side effects.
     */
    fun getOrNull(): DbParam? = contextParam.get()

    /**
     * Completely clears the current thread's [DbParam] binding (`ThreadLocal.remove`).
     * **Must be called at the end of every task in thread-pool scenarios**, otherwise
     * a reused thread will carry the old [DbParam] into the next task.
     */
    fun clear() {
        contextParam.remove()
    }

}
