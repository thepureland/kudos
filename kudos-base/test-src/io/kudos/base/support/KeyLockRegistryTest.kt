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
                val start = System.nanoTime()   // ← 进入临界区后再记时
                Thread.sleep(150)
                val end = System.nanoTime()
                i1.set(Interval(start, end))
            }
        }

        val t2 = pool.submit {
            barrier.await()
            registry.withLock(key) {
                val start = System.nanoTime()   // ← 进入临界区后再记时
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

        // 现在应该不重叠：同一个 key 的访问被串行化
        assertFalse(overlap(a, b))

        // 可选：再断言顺序
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
        // withLock 的 block 抛异常时，引用计数与锁映射必须被清理
        val key = "exception_test_key"
        val ex = runCatching {
            registry.withLock(key) {
                throw IllegalStateException("boom")
            }
        }
        assertTrue(ex.isFailure, "block 抛出的异常应原样向外传播")
        assertTrue(ex.exceptionOrNull() is IllegalStateException)
        assertEquals(0, registry.getActiveLockCount(), "block 抛异常后锁映射应被清理")
    }

    @Test
    fun unlockUnknownKeyShouldThrow() {
        // KDoc 约定 @throws IllegalStateException 当 key 不存在
        val outcome = runCatching { registry.unlock("never_locked_key") }
        assertTrue(outcome.isFailure)
        assertTrue(
            outcome.exceptionOrNull() is IllegalStateException,
            "不存在的 key unlock 应抛 IllegalStateException"
        )
    }

    @Test
    fun unlockFromNonHoldingThreadShouldNotThrow() {
        // 设计：unlock 内部用 `if (wrapper.lock.isHeldByCurrentThread)` 守卫，
        // 非持有线程调用 unlock 应安静返回，不抛锁状态异常
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
        // 其它线程对同一个 key 调 unlock：不持有锁，不应抛异常，但会扣引用计数
        // 用 submit<Result<Unit>> 显式选 Callable 重载，避免被推断成 Runnable
        val outcome: Result<Unit> = pool.submit<Result<Unit>> {
            runCatching { registry.unlock(key) }
        }.get(2, TimeUnit.SECONDS)
        assertTrue(outcome.isSuccess, "非持有线程 unlock 不应抛异常：${outcome.exceptionOrNull()}")

        canFinish.countDown()
        holder.get(2, TimeUnit.SECONDS)
        pool.shutdown()
    }

    @Test
    fun tryLockFailureShouldNotLeakLockMapEntry() {
        // tryLock 失败时引用计数应被回滚（KDoc："如果获取失败，引用计数会自动释放"）
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

        // 反复 tryLock 失败不应让 active count 持续增长
        repeat(10) {
            val attempt = registry.tryLock(key)
            assertNull(attempt, "锁被占用时 tryLock 应失败")
        }
        // 持锁中：active = 1（holder 自己的引用），失败的 tryLock 们已经清理
        assertEquals(1, registry.getActiveLockCount(), "tryLock 失败不应累积 active count")

        holder.get(2, TimeUnit.SECONDS)
        pool.shutdown()
        assertEquals(0, registry.getActiveLockCount(), "holder 完成后应彻底清理")
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
