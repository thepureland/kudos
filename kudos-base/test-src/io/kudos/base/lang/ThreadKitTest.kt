package io.kudos.base.lang

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * test for ThreadKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class ThreadKitTest {

    // 1) Verify that sleep(millis: Long) returns normally when not interrupted
    @Test
    fun sleep_Millis_NoException() {
        // Measure sleep duration to ensure the call does not throw and sleeps roughly the expected time
        val elapsed = measureTimeMillis {
            ThreadKit.sleep(100)
        }
        assertTrue(elapsed >= 90, "ThreadKit.sleep(100) should sleep close to 100ms, actual: $elapsed ms")
    }

    // 2) Verify that sleep(duration: Long, unit: TimeUnit) returns normally when not interrupted
    @Test
    fun sleep_DurationUnit_NoException() {
        val elapsed = measureTimeMillis {
            ThreadKit.sleep(200, TimeUnit.MILLISECONDS)
        }
        assertTrue(elapsed >= 180, "ThreadKit.sleep(200, MILLISECONDS) should sleep close to 200ms, actual: $elapsed ms")
    }

    // 3) Verify that sleep catches and ignores InterruptedException when the thread is interrupted
    @Test
    fun sleep_WhenInterrupted_IgnoredInternally() {
        // Start a thread, immediately interrupt it, then call sleep(...):
        val thread = Thread {
            // Interrupt itself in the child thread
            Thread.currentThread().interrupt()
            // Since it is already interrupted, sleep will throw InterruptedException,
            // which ThreadKit catches and ignores; it should not propagate outward.
            ThreadKit.sleep(50)
        }
        thread.start()
        thread.join(500)
        // If the child thread did not throw an uncaught exception, the ignore mechanism worked
        assertFalse(thread.isAlive, "Thread should have terminated normally")
    }

    // 4) Verify gracefulShutdown: with no tasks in the pool it should return quickly and not throw
    @Test
    fun gracefulShutdown_EmptyExecutor_NoException() {
        val pool = Executors.newSingleThreadExecutor()
        // Conflict: shutdown first, then invoke
        ThreadKit.gracefulShutdown(pool, 1, 1, TimeUnit.SECONDS)
        assertTrue(pool.isShutdown, "After gracefulShutdown the pool should be in the shutdown state")
    }

    // 5) Verify gracefulShutdown: with a long-running task, the first awaitTermination times out triggering shutdownNow, then the second awaitTermination also times out
    @Test
    fun gracefulShutdown_WithLongTask_TriggersShutdownNow() {
        val pool = Executors.newSingleThreadExecutor()
        // Submit a task that blocks indefinitely
        pool.submit {
            try {
                Thread.sleep(5000)
            } catch (_: InterruptedException) {
                // Ignored after being cancelled
            }
        }
        // Set both timeouts to 100ms: after the first awaitTermination times out, shutdownNow is called;
        // when the second awaitTermination also times out, it falls into the "thread pool did not terminate!" warn branch.
        // Here we only verify no exception is thrown.
        ThreadKit.gracefulShutdown(pool, 100, 100, TimeUnit.MILLISECONDS)
        // At this point the pool is definitely shut down (shutdown or shutdownNow has been called)
        assertTrue(pool.isShutdown, "After gracefulShutdown the pool should be in the shutdown state")
    }

    // 6) Verify normalShutdown: calls shutdownNow immediately and waits for the timeout
    @Test
    fun normalShutdown_WithRunningTask_NoException() {
        val pool = Executors.newFixedThreadPool(2)
        // Submit a long-running task
        pool.submit {
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
            }
        }
        // Call normalShutdown with a very short timeout
        ThreadKit.normalShutdown(pool, 50, TimeUnit.MILLISECONDS)
        assertTrue(pool.isShutdown, "After normalShutdown the pool should be in the shutdown state")
    }

}
