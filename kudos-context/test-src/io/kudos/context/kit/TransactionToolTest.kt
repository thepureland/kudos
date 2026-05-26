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
 * Tests for TransactionTool.
 *
 * Spring's [TransactionSynchronizationManager] is a ThreadLocal static API designed for testability:
 * - `initSynchronization()` lets the current thread simulate "inside a transaction"
 * - `setActualTransactionActive(true)` simulates a "real transaction"
 * - `clear()` clears the ThreadLocal state
 *
 * Coverage:
 * - The three paths of [TransactionTool.doAfterTransactionCommit]: register a sync inside a transaction / execute immediately when there is no transaction / sync-callback exception is swallowed
 * - Both states of [TransactionTool.hasTransaction]'s real-transaction flag
 *
 * @author K
 * @since 1.0.0
 */
internal class TransactionToolTest {

    @BeforeTest
    fun setupSyncContext() {
        // Each test case independently simulates "transaction synchronization is active"
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterTest
    fun cleanup() {
        // Clear all ThreadLocal state at the end of the test to avoid polluting the next case
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
        // At this point isSynchronizationActive=true (initialized by @BeforeTest)
        val ran = AtomicBoolean(false)
        TransactionTool.doAfterTransactionCommit { ran.set(true) }

        // Registration should not run immediately — it should wait for the afterCommit callback
        assertFalse(ran.get(), "inside a transaction: runnable should be deferred to afterCommit")

        // Simulate transaction commit: manually trigger afterCommit on all registered synchronizations
        val syncs = TransactionSynchronizationManager.getSynchronizations()
        assertEquals(1, syncs.size, "exactly one TransactionSynchronization should be registered")
        syncs.forEach { it.afterCommit() }

        assertTrue(ran.get(), "the runnable should only run after afterCommit fires")
    }

    @Test
    fun doAfterTransactionCommit_runsImmediatelyWhenNoSync() {
        // Turn off synchronization: simulate a non-transactional context
        TransactionSynchronizationManager.clearSynchronization()
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive())

        val ran = AtomicBoolean(false)
        TransactionTool.doAfterTransactionCommit { ran.set(true) }

        assertTrue(ran.get(), "non-transactional context: runnable should run immediately")
    }

    @Test
    fun doAfterTransactionCommit_swallowsRunnableException() {
        // In the sync path, exceptions from the runnable should be swallowed (so they do not affect other syncs)
        TransactionTool.doAfterTransactionCommit { throw IllegalStateException("intentional") }

        val syncs = TransactionSynchronizationManager.getSynchronizations()
        // Triggering afterCommit should not throw out
        val outcome = runCatching {
            syncs.forEach { it.afterCommit() }
        }
        assertTrue(outcome.isSuccess, "runnable exception should be caught and not propagate: ${outcome.exceptionOrNull()}")
    }

    @Test
    fun doAfterTransactionCommit_eachCallRegistersIndependentSync() {
        val counter = AtomicInteger(0)
        repeat(3) {
            TransactionTool.doAfterTransactionCommit { counter.incrementAndGet() }
        }

        val syncs = TransactionSynchronizationManager.getSynchronizations()
        assertEquals(3, syncs.size, "each call should register an independent synchronization")

        syncs.forEach { it.afterCommit() }
        assertEquals(3, counter.get(), "all three callbacks should fire")
    }

    @Test
    fun doAfterTransactionCommit_oneFailureDoesNotBlockOthers() {
        // Verify: "an exception in one sync does not block the execution of another sync" — this is the core value
        // of the try-catch inside TransactionTool.
        val ranAfterBadOne = AtomicBoolean(false)
        TransactionTool.doAfterTransactionCommit { throw RuntimeException("first one fails") }
        TransactionTool.doAfterTransactionCommit { ranAfterBadOne.set(true) }

        val syncs = TransactionSynchronizationManager.getSynchronizations()
        syncs.forEach { runCatching { it.afterCommit() } }

        assertTrue(ranAfterBadOne.get(), "an exception in the previous sync should not prevent the next one from running")
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
        // Synchronization is active but there is no "real transaction" — hasTransaction should return false.
        // This is the key distinction between isSynchronizationActive and isActualTransactionActive in Spring.
        assertTrue(TransactionSynchronizationManager.isSynchronizationActive())
        TransactionSynchronizationManager.setActualTransactionActive(false)
        assertFalse(TransactionTool.hasTransaction(), "sync active but no real transaction -> hasTransaction=false")
    }
}
