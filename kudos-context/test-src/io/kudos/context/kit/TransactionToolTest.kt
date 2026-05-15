package io.kudos.context.kit

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * TransactionTool 测试用例
 *
 * Spring 的 [TransactionSynchronizationManager] 是 ThreadLocal 静态 API，专为可测设计：
 * - `initSynchronization()` 让当前线程模拟"在事务里"
 * - `setActualTransactionActive(true)` 模拟"真实事务"
 * - `clear()` 清掉 ThreadLocal 状态
 *
 * 覆盖：
 * - [TransactionTool.doAfterTransactionCommit] 三种路径：事务里注册同步 / 无事务立即执行 / 同步回调中异常被吞
 * - [TransactionTool.hasTransaction] 真实事务标志的两种状态
 *
 * @author K
 * @since 1.0.0
 */
internal class TransactionToolTest {

    @BeforeTest
    fun setupSyncContext() {
        // 每个用例独立模拟"事务同步处于激活态"
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterTest
    fun cleanup() {
        // 测试结束清掉所有 ThreadLocal，避免污染下一个 case
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    // ============================================================
    // doAfterTransactionCommit
    // ============================================================

    @Test
    fun doAfterTransactionCommit_registersSynchronizationWhenActive() {
        // 此时 isSynchronizationActive=true（由 @BeforeTest 初始化）
        val ran = AtomicBoolean(false)
        TransactionTool.doAfterTransactionCommit { ran.set(true) }

        // 注册时不应立即跑——要等事务提交后回调
        assertFalse(ran.get(), "事务中：runnable 应延迟到 afterCommit")

        // 模拟事务提交：手动触发所有注册的同步对象的 afterCommit
        val syncs = TransactionSynchronizationManager.getSynchronizations()
        assertEquals(1, syncs.size, "应注册了一个 TransactionSynchronization")
        syncs.forEach { it.afterCommit() }

        assertTrue(ran.get(), "afterCommit 触发后 runnable 才执行")
    }

    @Test
    fun doAfterTransactionCommit_runsImmediatelyWhenNoSync() {
        // 关掉同步：模拟非事务上下文
        TransactionSynchronizationManager.clearSynchronization()
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive())

        val ran = AtomicBoolean(false)
        TransactionTool.doAfterTransactionCommit { ran.set(true) }

        assertTrue(ran.get(), "非事务上下文：runnable 立即执行")
    }

    @Test
    fun doAfterTransactionCommit_swallowsRunnableException() {
        // 同步路径下 runnable 抛异常应被吞掉（不影响其它同步对象）
        TransactionTool.doAfterTransactionCommit { throw IllegalStateException("intentional") }

        val syncs = TransactionSynchronizationManager.getSynchronizations()
        // 触发 afterCommit 不应向外抛
        val outcome = runCatching {
            syncs.forEach { it.afterCommit() }
        }
        assertTrue(outcome.isSuccess, "runnable 异常应被 catch 不外抛：${outcome.exceptionOrNull()}")
    }

    @Test
    fun doAfterTransactionCommit_eachCallRegistersIndependentSync() {
        val counter = AtomicInteger(0)
        repeat(3) {
            TransactionTool.doAfterTransactionCommit { counter.incrementAndGet() }
        }

        val syncs = TransactionSynchronizationManager.getSynchronizations()
        assertEquals(3, syncs.size, "每次调用应注册独立的 synchronization")

        syncs.forEach { it.afterCommit() }
        assertEquals(3, counter.get(), "三次回调应都被触发")
    }

    @Test
    fun doAfterTransactionCommit_oneFailureDoesNotBlockOthers() {
        // 验证："其中一个 sync 抛异常不影响另一个 sync 的执行"——这是 TransactionTool
        // 内部 try-catch 设计的核心价值。
        val ranAfterBadOne = AtomicBoolean(false)
        TransactionTool.doAfterTransactionCommit { throw RuntimeException("first one fails") }
        TransactionTool.doAfterTransactionCommit { ranAfterBadOne.set(true) }

        val syncs = TransactionSynchronizationManager.getSynchronizations()
        syncs.forEach { runCatching { it.afterCommit() } }

        assertTrue(ranAfterBadOne.get(), "前一个 sync 抛异常不应阻止后一个执行")
    }

    // ============================================================
    // hasTransaction
    // ============================================================

    @Test
    fun hasTransaction_returnsTrueWhenActualTransactionActive() {
        TransactionSynchronizationManager.setActualTransactionActive(true)
        assertTrue(TransactionTool.hasTransaction())
    }

    @Test
    fun hasTransaction_returnsFalseWhenNoActualTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false)
        assertFalse(TransactionTool.hasTransaction())
    }

    @Test
    fun hasTransaction_independentFromSyncActive() {
        // synchronization 激活但没"真实事务"——hasTransaction 应返回 false
        // 这是 Spring 区分 isSynchronizationActive vs isActualTransactionActive 的关键
        assertTrue(TransactionSynchronizationManager.isSynchronizationActive())
        TransactionSynchronizationManager.setActualTransactionActive(false)
        assertFalse(TransactionTool.hasTransaction(), "sync 激活但无真实事务 → hasTransaction=false")
    }
}
