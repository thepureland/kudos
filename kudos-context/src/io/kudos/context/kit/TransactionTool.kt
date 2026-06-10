package io.kudos.context.kit

import io.kudos.base.logger.LogFactory
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Spring transaction utilities.
 * Wraps the common "run a side effect only after the transaction commits (publish MQ, evict cache,
 * call an external API)" pattern into a one-liner. Falls back to immediate execution when no real
 * transaction is present, so business code does not need to scatter [TransactionSynchronizationManager]
 * boilerplate everywhere.
 *
 * @author K
 * @since 1.0.0
 */
object TransactionTool {
    /** Logger, used only to record exceptions when a callback fails. */
    private val log = LogFactory.getLog(TransactionTool::class)

    /**
     * Register a callback that runs only after the transaction commits successfully.
     * If the current thread is not in a transaction, runs immediately; otherwise registers it with
     * [TransactionSynchronizationManager] so Spring invokes it during afterCommit. Exceptions inside
     * the callback are only logged, not propagated.
     *
     * @param r the logic to defer
     * @author K
     * @since 1.0.0
     */
    fun doAfterTransactionCommit(r: Runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        try {
                            r.run()
                        } catch (e: Exception) {
                            log.error(e, "afterCommit execution failed")
                        }
                    }
                }
            )
        } else {
            r.run()
        }
    }

    /**
     * Whether the current thread is in a real transaction.
     * The difference from [TransactionSynchronizationManager.isSynchronizationActive] is that the
     * latter also returns true when only synchronization is registered but no actual transaction has
     * been opened; this method only returns true when a real transaction is active.
     *
     * @return true if a real active transaction exists
     * @author K
     * @since 1.0.0
     */
    fun hasTransaction(): Boolean = TransactionSynchronizationManager.isActualTransactionActive()
}
