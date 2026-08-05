package io.kudos.ability.distributed.lock.redisson.atom

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the deprecated [AtomExecuteTask], covering status transitions on
 * run/stopTask, runnable delegation and the null-runnable constructor.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Suppress("DEPRECATION")
internal class AtomExecuteTaskTest {

    @Test
    fun statusIsFalseBeforeRun() {
        val task = AtomExecuteTask(null)
        assertFalse(task.status)
    }

    @Test
    fun run_setsStatusTrueAndExecutesRunnable() {
        val counter = AtomicInteger(0)
        val task = AtomExecuteTask { counter.incrementAndGet() }

        // Invoke run() directly (same thread) to keep the test deterministic.
        task.run()

        assertTrue(task.status)
        assertEquals(1, counter.get())
    }

    @Test
    fun run_withNullRunnable_setsStatusTrueWithoutError() {
        val task = AtomExecuteTask(null)
        task.run()
        assertTrue(task.status)
    }

    @Test
    fun stopTask_resetsStatusToFalse() {
        val task = AtomExecuteTask { }
        task.run()
        assertTrue(task.status)

        task.stopTask()

        assertFalse(task.status)
    }
}
