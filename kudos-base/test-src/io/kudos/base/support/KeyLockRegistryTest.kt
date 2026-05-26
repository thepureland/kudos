package io.kudos.base.support

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis
import kotlin.test.*

/**
 * junit for KeyLockRegistry
 *
 * @author AI: ChatGPT
 * @since 1.0.0
 */
class KeyLockRegistryTest {

    private val registry = KeyLockRegistry<String>()

    @Test
    fun shouldSerializeAccessForSameKey() {
        val pool = Executors.newFixedThreadPool(2)
        val barrier = CyclicBarrier(2)
        val key = "K1"

        data class Interval(val start: Long, val end: Long)

        val i1 = AtomicReference<Interval?>()
        val i2 = AtomicReference<Interval?>()

        val t1 = pool.submit {
            barrier.await()
            registry.withLock(key) {
                val start = System.nanoTime()   // <- start timing after entering the critical section
                Thread.sleep(150)
                val end = System.nanoTime()
                i1.set(Interval(start, end))
            }
        }

        val t2 = pool.submit {
            barrier.await()
            registry.withLock(key) {
                val start = System.nanoTime()   // <- start timing after entering the critical section
                Thread.sleep(150)
                val end = System.nanoTime()
                i2.set(Interval(start, end))
            }
        }

        t1.get(2, TimeUnit.SECONDS)
        t2.get(2, TimeUnit.SECONDS)
        pool.shutdown()

        val a = requireNotNull(i1.get())
        val b = requireNotNull(i2.get())

        fun overlap(x: Interval, y: Interval) = !(x.end <= y.start || y.end <= x.start)

        // Should not overlap now: access to the same key is serialized
        assertFalse(overlap(a, b))

        // Optional: assert ordering as well
        assertTrue(a.end <= b.start || b.end <= a.start)

        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun shouldAllowParallelExecutionForDifferentKeys() {
        val pool = Executors.newFixedThreadPool(2)
        val barrier = CyclicBarrier(2)
        val k1 = "A"
        val k2 = "B"

        val elapsed = measureTimeMillis {
            val f1 = pool.submit { barrier.await(); registry.withLock(k1) { Thread.sleep(150) } }
            val f2 = pool.submit { barrier.await(); registry.withLock(k2) { Thread.sleep(150) } }
            f1.get(2, TimeUnit.SECONDS)
            f2.get(2, TimeUnit.SECONDS)
            pool.shutdown()
        }

        assertTrue(elapsed < 300)
        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun tryLockShouldFailWhenLockedAndSucceedAfterRelease() {
        val key = "T1"
        val latchHolding = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()

        val holder = pool.submit {
            registry.withLock(key) {
                latchHolding.countDown()
                Thread.sleep(200)
            }
        }

        assertTrue(latchHolding.await(1, TimeUnit.SECONDS))
        var lock = registry.tryLock(key)
        assertNull(lock)

        holder.get(2, TimeUnit.SECONDS)
        lock = registry.tryLock(key)
        assertNotNull(lock)
        registry.unlock(key)

        pool.shutdown()
        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun tryLockWithTimeoutShouldRespectTimeout() {
        val key = "T2"
        val pool = Executors.newSingleThreadExecutor()
        val latchHolding = CountDownLatch(1)

        val holder = pool.submit {
            registry.withLock(key) {
                latchHolding.countDown()
                Thread.sleep(200)
            }
        }

        assertTrue(latchHolding.await(1, TimeUnit.SECONDS))
        var lock = registry.tryLock(key, 50, TimeUnit.MILLISECONDS)
        assertNull(lock)

        holder.get(2, TimeUnit.SECONDS)
        lock = registry.tryLock(key, 200, TimeUnit.MILLISECONDS)
        assertNotNull(lock)
        registry.unlock(key)

        pool.shutdown()
        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun unlockShouldReleaseLockAndCleanup() {
        val key = "U1"
        val lock = registry.tryLock(key)
        assertNotNull(lock)
        assertEquals(1, registry.getActiveLockCount())

        registry.unlock(key)
        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun shouldSupportReentrancy() {
        val key = "R1"
        val order = mutableListOf<String>()
        registry.withLock(key) {
            order += "outer:start"
            registry.withLock(key) {
                order += "inner"
            }
            order += "outer:end"
        }
        assertEquals(listOf("outer:start", "inner", "outer:end"), order)
        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun blockExceptionShouldStillReleaseLockAndCleanup() {
        // When withLock's block throws, the reference count and lock map must be cleaned up
        val key = "exception_test_key"
        val ex = runCatching {
            registry.withLock(key) {
                throw IllegalStateException("boom")
            }
        }
        assertTrue(ex.isFailure, "Exceptions thrown by the block should propagate unchanged")
        assertTrue(ex.exceptionOrNull() is IllegalStateException)
        assertEquals(0, registry.getActiveLockCount(), "Lock map should be cleaned up after the block throws")
    }

    @Test
    fun unlockUnknownKeyShouldThrow() {
        // KDoc states @throws IllegalStateException when the key does not exist
        val outcome = runCatching { registry.unlock("never_locked_key") }
        assertTrue(outcome.isFailure)
        assertTrue(
            outcome.exceptionOrNull() is IllegalStateException,
            "unlock with a non-existent key should throw IllegalStateException"
        )
    }

    @Test
    fun unlockFromNonHoldingThreadShouldNotThrow() {
        // By design: unlock guards with `if (wrapper.lock.isHeldByCurrentThread)`;
        // calling unlock from a non-holding thread should return quietly without throwing a lock-state exception
        val key = "non_holding_thread_test"
        val pool = Executors.newFixedThreadPool(2)
        val holdingStarted = CountDownLatch(1)
        val canFinish = CountDownLatch(1)

        val holder = pool.submit {
            registry.withLock(key) {
                holdingStarted.countDown()
                canFinish.await(2, TimeUnit.SECONDS)
            }
        }

        assertTrue(holdingStarted.await(1, TimeUnit.SECONDS))
        // Another thread calls unlock on the same key: it does not hold the lock, must not throw, but decrements the ref count
        // Use submit<Result<Unit>> to explicitly pick the Callable overload, preventing inference as Runnable
        val outcome: Result<Unit> = pool.submit<Result<Unit>> {
            runCatching { registry.unlock(key) }
        }.get(2, TimeUnit.SECONDS)
        assertTrue(outcome.isSuccess, "unlock from a non-holding thread should not throw: ${outcome.exceptionOrNull()}")

        canFinish.countDown()
        holder.get(2, TimeUnit.SECONDS)
        pool.shutdown()
    }

    @Test
    fun tryLockFailureShouldNotLeakLockMapEntry() {
        // When tryLock fails, the reference count should be rolled back (KDoc: "if acquisition fails, the reference count is released automatically")
        val key = "no_leak_on_failure_key"
        val pool = Executors.newSingleThreadExecutor()
        val holdingStarted = CountDownLatch(1)

        val holder = pool.submit {
            registry.withLock(key) {
                holdingStarted.countDown()
                Thread.sleep(150)
            }
        }
        assertTrue(holdingStarted.await(1, TimeUnit.SECONDS))

        // Repeated tryLock failures should not let active count grow indefinitely
        repeat(10) {
            val attempt = registry.tryLock(key)
            assertNull(attempt, "tryLock should fail while the lock is held")
        }
        // While holding: active = 1 (holder's own ref); the failed tryLocks have been cleaned up
        assertEquals(1, registry.getActiveLockCount(), "Failed tryLock should not accumulate active count")

        holder.get(2, TimeUnit.SECONDS)
        pool.shutdown()
        assertEquals(0, registry.getActiveLockCount(), "Should be fully cleaned up after holder completes")
    }

    @Test
    fun shouldIncrementCounterCorrectlyUnderContention() {
        val key = "CNT"
        val pool = Executors.newFixedThreadPool(8)
        val barrier = CyclicBarrier(8)
        val loops = 100
        val counter = AtomicInteger(0)

        val tasks = (1..8).map {
            pool.submit<Unit> {
                barrier.await()
                repeat(loops) {
                    registry.withLock(key) {
                        counter.incrementAndGet()
                    }
                }
            }
        }
        tasks.forEach { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(8 * loops, counter.get())
        assertEquals(0, registry.getActiveLockCount())
    }

}
