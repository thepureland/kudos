package io.kudos.ability.data.rdb.ktorm.rowscope


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
 * Uses an [InheritableThreadLocal] so a child thread spawned inside the block inherits the
 * marker — the common case of a job fanning out. A thread pool that outlives the block must clear
 * it, which [runAsSystem] does for its own frame.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object DataScopeContext {

    private val systemDepth = InheritableThreadLocal.withInitial { 0 }

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
