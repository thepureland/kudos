package io.kudos.ability.data.rdb.ktorm.rowscope

import java.util.concurrent.Callable


/**
 * Marks a stretch of work as running with **system authority**, so row filtering does not apply.
 *
 * ```kotlin
 * DataScopeContext.runAsSystem { orderDao.search(criteria) }
 * ```
 *
 * **Why this has to be explicit.** Scheduled jobs, message consumers and service-to-service calls
 * legitimately have no logged-in subject, and something has to happen when the filter finds none.
 * Of the three possible answers only this one is safe:
 *
 * - *no subject ⇒ do not filter* — unacceptable. Every path that merely **loses** the context
 *   (async dispatch, thread pool, Feign call) would then silently read everything, and losing the
 *   context is both the easiest mistake to make and the hardest to notice.
 * - *no subject ⇒ see nothing* — safe, but a scheduled job that quietly returns zero rows becomes a
 *   class of outage nobody can diagnose.
 * - *no subject and no declaration ⇒ fail loudly* — a forgotten declaration surfaces during rollout
 *   as an error, instead of as a data leak or an empty report months later.
 *
 * **The marker is confined to the thread that set it**, and crossing a thread boundary is explicit:
 * see [wrap]. An [InheritableThreadLocal] would look more convenient and is the wrong trade here —
 * inheritance is decided when a thread is *created*, so a pooled worker that happens to be spun up
 * inside a `runAsSystem` block keeps system authority for the rest of its life, serving unrelated
 * requests unfiltered. That failure is silent, permanent, and impossible to reproduce on demand;
 * a forgotten [wrap] fails loudly on the first call instead.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object DataScopeContext {

    private val systemDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

    /** Whether the current thread is running with system authority. */
    @JvmStatic
    fun isSystem(): Boolean = systemDepth.get() > 0

    /**
     * Runs [block] with system authority. Re-entrant: nested calls do not clear the marker early.
     *
     * @param block the work to run unfiltered
     * @return whatever [block] returns
     */
    @JvmStatic
    fun <R> runAsSystem(block: () -> R): R {
        systemDepth.set(systemDepth.get() + 1)
        try {
            return block()
        } finally {
            val remaining = systemDepth.get() - 1
            if (remaining <= 0) systemDepth.remove() else systemDepth.set(remaining)
        }
    }

    /**
     * Captures the calling thread's authority and returns [task] re-wrapped to run under it, for the
     * case of a system-authority job fanning work out to other threads.
     *
     * ```kotlin
     * DataScopeContext.runAsSystem {
     *     ids.map { id -> executor.submit(DataScopeContext.wrap(Callable { dao.get(id) })) }
     * }
     * ```
     *
     * Capture happens now, on the calling thread; the authority is released again when the wrapped
     * task returns, so handing the task to a pooled thread leaves nothing behind on it.
     *
     * @param task the work to run on another thread
     * @return the same work, carrying this thread's authority
     */
    @JvmStatic
    fun <V> wrap(task: Callable<V>): Callable<V> {
        if (!isSystem()) return task
        return Callable { runAsSystem { task.call() } }
    }

    /** [wrap] for work that returns nothing. */
    @JvmStatic
    fun wrap(task: Runnable): Runnable {
        if (!isSystem()) return task
        return Runnable { runAsSystem { task.run() } }
    }
}


/**
 * Thrown when a query touches a row-scoped entity with no subject and no declared system authority.
 *
 * Deliberately not a silent empty result: see [DataScopeContext] for why the loud failure is the
 * safe answer.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RowScopeUnresolvedException(message: String) : IllegalStateException(message)
