package io.kudos.context.lock

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for NormalLockService.
 *
 * Coverage:
 * - Lease lock [NormalLockService.tryLock]: success / re-entrance on the same key fails / different keys in parallel
 * - Automatic expiration cleanup — waits for the daemon thread to remove expired keys from memory
 * - The independent behavior of the reentrant lock [NormalLockService.lock] / [NormalLockService.unLock]
 * - The try-finally release in [NormalLockService.lockExecute]
 *
 * Decoupled from LockTool / Spring context — directly instantiates NormalLockService() to test the pure logic.
 *
 * @author K
 * @since 1.0.0
 */
internal class NormalLockServiceTest {

    // ============================================================
    // Lease lock basic semantics
    // ============================================================

    @Test
    fun tryLockSucceedsForUnlockedKey() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k1", 10))
    }

    @Test
    fun tryLockFailsForLockedKey() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k1", 10))
        // tryLock on the same key again: fails (lease lock is non-reentrant)
        assertFalse(svc.tryLock("k1", 10))
    }

    @Test
    fun tryLockSucceedsForDifferentKeys() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k1", 10))
        assertTrue(svc.tryLock("k2", 10), "different keys should not affect each other")
        assertTrue(svc.tryLock("k3", 10))
    }

    @Test
    fun unLockReleasesKey() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k1", 60))
        assertFalse(svc.tryLock("k1", 60), "should fail before release")
        svc.unLock("k1")
        assertTrue(svc.tryLock("k1", 60), "should be able to tryLock again after release")
    }

    // ============================================================
    // Expiration cleanup: daemon thread
    // ============================================================

    @Test
    fun expiredLeaseIsAutomaticallyReleased() {
        val svc = NormalLockService()
        // Set 1-second expiration
        assertTrue(svc.tryLock("expiring-key", 1))
        // Immediate retry should fail
        assertFalse(svc.tryLock("expiring-key", 1))
        // Wait for the daemon thread to clean up after expiration (with some buffer)
        Thread.sleep(1500)
        assertTrue(
            svc.tryLock("expiring-key", 1),
            "after 1-second expiration the daemon thread should have cleaned up and the key should be reacquirable"
        )
    }

    // ============================================================
    // lockExecute automatic try-finally release
    // ============================================================

    @Test
    fun lockExecuteAutoReleasesAfterSupplier() {
        val svc = NormalLockService()
        val result = svc.lockExecute(
            lockKey = "lk",
            supplier = { "computed-value" },
            sec = 30,
            errorCode = null
        )
        assertEquals("computed-value", result)
        // The lock should be released after the supplier completes, so it can be acquired again
        assertTrue(svc.tryLock("lk", 30), "lockExecute should release the lock after the supplier")
    }

    @Test
    fun lockExecuteReleasesEvenOnSupplierException() {
        val svc = NormalLockService()
        val outcome = runCatching {
            svc.lockExecute<String>(
                lockKey = "lk",
                supplier = { throw IllegalStateException("boom") },
                sec = 30,
                errorCode = null
            )
        }
        assertTrue(outcome.isFailure)
        assertTrue(outcome.exceptionOrNull() is IllegalStateException)
        // Even on exception, the finally block must release
        assertTrue(svc.tryLock("lk", 30), "lockExecute must release the lock even on the exception path")
    }

    @Test
    fun lockExecuteReturnsNullWhenAcquireFailsAndNoErrorCode() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k", 60), "preempt first")
        // Already held by someone else; errorCode=null -> should return null instead of throwing
        val result = svc.lockExecute(
            lockKey = "k",
            supplier = { "should-not-run" },
            sec = 1,
            errorCode = null
        )
        assertNull(result)
    }

    // ============================================================
    // Reentrant lock (lock/unLock(lock, key)) is decoupled from the lease lock (tryLock)
    // ============================================================

    @Test
    fun reentrantLockAndLeaseLockAreIndependentMechanisms() {
        // This test pins down the design where the "two mechanisms do not affect each other" — holding both
        // a lease lock and a reentrant lock on the same key does not conflict, because they use separate data structures.
        val svc = NormalLockService()
        assertTrue(svc.tryLock("dual", 60), "acquire the lease lock")
        val reentrant = svc.lock("dual")
        assertNotNull(reentrant, "acquiring the reentrant lock should not be blocked by the lease lock")
        svc.unLock(reentrant, "dual")
    }

    // ============================================================
    // doCache / hasKey / unLock edge cases
    // ============================================================

    @Test
    fun doCachePutsKeyAndHasKeyObservesIt() {
        val svc = NormalLockService()
        assertFalse(svc.hasKey("cached"), "unknown key should not exist")
        svc.doCache("cached", 60)
        assertTrue(svc.hasKey("cached"), "doCache should hold the key")
        assertFalse(svc.tryLock("cached", 60), "doCache occupies the key just like tryLock")
    }

    // Note: hasKey(null) currently throws NullPointerException (ConcurrentHashMap.containsKey rejects
    // null) even though the parameter type is String?. Suspected main-code bug — the nullable signature
    // implies "null never stored -> false". Not asserted here to avoid pinning the wrong behavior.

    @Test
    fun unLockOnAbsentKeyIsNoOp() {
        val svc = NormalLockService()
        svc.unLock("never-locked") // must not throw
        assertTrue(svc.tryLock("never-locked", 30))
    }

    @Test
    fun unLockRemovesStaleDelayQueueEntrySoNewLockSurvivesOldExpiry() {
        // Pin down the documented fix: after unLock, the old delay-queue entry must not later
        // wipe out a NEW lock that reuses the same key.
        val svc = NormalLockService()
        assertTrue(svc.tryLock("reused", 1), "old lock with 1s lease")
        svc.unLock("reused")
        assertTrue(svc.tryLock("reused", 60), "re-acquire the same key with a long lease")
        Thread.sleep(1500) // wait past the OLD lock's expiry
        assertFalse(
            svc.tryLock("reused", 60),
            "the new lock must still be held: the stale delay-queue entry must not have cleaned it"
        )
    }

    @Test
    fun reentrantLockReturnsNullWhenHeldByAnotherThread() {
        val svc = NormalLockService()
        val acquired = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = Thread {
            val lock = svc.lock("contended-reentrant")
            acquired.countDown()
            if (lock != null) {
                release.await(5, TimeUnit.SECONDS)
                svc.unLock(lock, "contended-reentrant")
            }
        }
        holder.start()
        assertTrue(acquired.await(5, TimeUnit.SECONDS))
        assertNull(svc.lock("contended-reentrant"), "a key held by another thread must yield null")
        release.countDown()
        holder.join(5000)
    }

    // ============================================================
    // Daemon thread: interruption handling
    // ============================================================

    @Test
    fun expiryDaemonThreadStopsCleanlyOnInterrupt() {
        val before = Thread.getAllStackTraces().keys.filterTo(HashSet()) { it.name == "kudos-normal-lock-expiry" }
        NormalLockService() // spawns exactly one new expiry daemon
        val daemon = Thread.getAllStackTraces().keys
            .first { it.name == "kudos-normal-lock-expiry" && it !in before }
        assertTrue(daemon.isDaemon, "the expiry thread must be a daemon")

        daemon.interrupt()
        daemon.join(5000)
        assertFalse(daemon.isAlive, "the daemon must exit its loop when interrupted")
    }

    // ============================================================
    // ExpiringKey: key-only equality contract (private class -> exercised reflectively, the same
    // contract DelayQueue.remove relies on)
    // ============================================================

    @Test
    fun expiringKeyEqualityAndHashAreKeyOnly() {
        val clazz = Class.forName("io.kudos.context.lock.NormalLockService\$ExpiringKey")
        val ctor = clazz.getDeclaredConstructor(Any::class.java, Long::class.javaPrimitiveType)
        ctor.isAccessible = true
        val now = System.currentTimeMillis()
        val a1 = ctor.newInstance("same-key", now + 1000)
        val a2 = ctor.newInstance("same-key", now + 99999) // different expiry, same key
        val b = ctor.newInstance("other-key", now + 1000)

        assertEquals(a1, a1, "reflexive")
        assertEquals(a1, a2, "equality must ignore the expiry timestamp")
        assertFalse(a1 == b, "different keys are not equal")
        assertFalse(a1.equals("not-an-expiring-key"), "foreign types are not equal")
        assertEquals(a1.hashCode(), a2.hashCode(), "hashCode must be consistent with key-only equals")
        assertEquals("same-key".hashCode(), a1.hashCode())
    }

    // ============================================================
    // Concurrent contention: multiple threads tryLock the same key — exactly one should succeed
    // ============================================================

    @Test
    fun onlyOneThreadWinsTryLockUnderContention() {
        val svc = NormalLockService()
        val threadCount = 16
        val pool = Executors.newFixedThreadPool(threadCount)
        val barrier = CountDownLatch(1)
        val successCount = AtomicInteger(0)
        val done = CountDownLatch(threadCount)

        repeat(threadCount) {
            pool.submit {
                barrier.await()
                if (svc.tryLock("contended", 60)) {
                    successCount.incrementAndGet()
                }
                done.countDown()
            }
        }
        barrier.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        pool.shutdown()
        assertEquals(1, successCount.get(), "under contention, only one thread should acquire the lock on the same key")
    }
}
