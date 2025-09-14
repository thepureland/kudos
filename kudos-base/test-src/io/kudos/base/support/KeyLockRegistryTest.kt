package io.kudos.base.support

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit for KeyLockRegistry
 *
 * @author ChatGPT
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

        // 用 AtomicReference 代替 @Volatile 局部变量
        val i1 = AtomicReference<Interval?>()
        val i2 = AtomicReference<Interval?>()

        val t1 = pool.submit {
            barrier.await()
            val start = System.nanoTime()
            registry.withLock(key) { Thread.sleep(150) }
            val end = System.nanoTime()
            i1.set(Interval(start, end))
        }

        val t2 = pool.submit {
            barrier.await()
            val start = System.nanoTime()
            registry.withLock(key) { Thread.sleep(150) }
            val end = System.nanoTime()
            i2.set(Interval(start, end))
        }

        t1.get(2, TimeUnit.SECONDS)
        t2.get(2, TimeUnit.SECONDS)
        pool.shutdown()

        val a = requireNotNull(i1.get())
        val b = requireNotNull(i2.get())
        fun overlap(x: Interval, y: Interval) = !(x.end <= y.start || y.end <= x.start)
        assertFalse(overlap(a, b))

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
        val acquiredWhileHeld = registry.tryLock(key)
        assertFalse(acquiredWhileHeld)

        holder.get(2, TimeUnit.SECONDS)
        val acquiredAfterRelease = registry.tryLock(key)
        assertTrue(acquiredAfterRelease)
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
        val failed = registry.tryLock(key, 50, TimeUnit.MILLISECONDS)
        assertFalse(failed)

        holder.get(2, TimeUnit.SECONDS)
        val success = registry.tryLock(key, 200, TimeUnit.MILLISECONDS)
        assertTrue(success)
        registry.unlock(key)

        pool.shutdown()
        assertEquals(0, registry.getActiveLockCount())
    }

    @Test
    fun unlockShouldReleaseLockAndCleanup() {
        val key = "U1"
        val ok = registry.tryLock(key)
        assertTrue(ok)
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
