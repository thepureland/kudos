package io.kudos.context.kit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

object TransactionTool {
    private val log: Log = LogFactory.getLog(TransactionTool::class.java)

    fun doAfterTransactionCommit(r: Runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        try {
                            r.run()
                        } catch (e: Exception) {
                            log.error("afterCommit 执行失败", e)
                        }
                    }
                }
            )
        } else {
            r.run()
        }
    }

    /**
     * 是否在真实事务中
     * @return
     */
    fun hasTransaction(): Boolean {
        return TransactionSynchronizationManager.isActualTransactionActive()
    }
}
