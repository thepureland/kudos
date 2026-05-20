package io.kudos.context.kit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Spring 事务相关工具。
 * 把"事务提交后才执行某段副作用（发 MQ、清缓存、调外部接口）"这种常见模式封装成一行调用，
 * 在没有真实事务时退化为立即执行，避免业务代码到处写 [TransactionSynchronizationManager] 样板。
 *
 * @author K
 * @since 1.0.0
 */
object TransactionTool {
    /** 日志器，仅用于回调执行失败时记录异常 */
    private val log: Log = LogFactory.getLog(TransactionTool::class.java)

    /**
     * 注册一段"事务提交成功后才执行"的回调。
     * 当前线程不在事务里时立即执行；否则注册到 [TransactionSynchronizationManager]，
     * 由 Spring 在 afterCommit 阶段调用。回调内异常仅记日志，不传播。
     *
     * @param r 待延后执行的逻辑
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
     * 当前线程是否处于真实事务中。
     * 与 [TransactionSynchronizationManager.isSynchronizationActive] 的区别在于：
     * 后者在仅注册了 synchronization 但没真开事务时也会返回 true；本方法只有真正在事务里才返回 true。
     *
     * @return true 表示存在真实活跃事务
     * @author K
     * @since 1.0.0
     */
    fun hasTransaction(): Boolean = TransactionSynchronizationManager.isActualTransactionActive()
}
