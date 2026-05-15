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
 * NormalLockService 测试用例
 *
 * 覆盖：
 * - 租约锁 [NormalLockService.tryLock]：成功 / 同 key 重入失败 / 不同 key 并行
 * - 过期自清理 —— 等待守护线程把过期 key 从内存里挪走
 * - 可重入锁 [NormalLockService.lock] / [NormalLockService.unLock] 的独立工作
 * - 与 [NormalLockService.lockExecute] 的 try-finally 释放
 *
 * 与 LockTool / Spring 上下文解耦——直接 new NormalLockService() 测纯逻辑。
 *
 * @author K
 * @since 1.0.0
 */
internal class NormalLockServiceTest {

    // ============================================================
    // 租约锁基础语义
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
        // 同一个 key 再次 tryLock：失败（租约锁不可重入）
        assertFalse(svc.tryLock("k1", 10))
    }

    @Test
    fun tryLockSucceedsForDifferentKeys() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k1", 10))
        assertTrue(svc.tryLock("k2", 10), "不同 key 互不影响")
        assertTrue(svc.tryLock("k3", 10))
    }

    @Test
    fun unLockReleasesKey() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k1", 60))
        assertFalse(svc.tryLock("k1", 60), "未释放前应失败")
        svc.unLock("k1")
        assertTrue(svc.tryLock("k1", 60), "释放后应能再次 tryLock")
    }

    // ============================================================
    // 过期自清理：守护线程
    // ============================================================

    @Test
    fun expiredLeaseIsAutomaticallyReleased() {
        val svc = NormalLockService()
        // 设置 1 秒过期
        assertTrue(svc.tryLock("expiring-key", 1))
        // 立即再试应失败
        assertFalse(svc.tryLock("expiring-key", 1))
        // 等过期后守护线程清理（给一定缓冲）
        Thread.sleep(1500)
        assertTrue(
            svc.tryLock("expiring-key", 1),
            "1 秒过期后守护线程应已清理，可重新获取"
        )
    }

    // ============================================================
    // lockExecute 自动 try-finally 释放
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
        // supplier 完成后锁已释放，应能再次获取
        assertTrue(svc.tryLock("lk", 30), "lockExecute 应在 supplier 后释放锁")
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
        // 即使异常，finally 也应释放
        assertTrue(svc.tryLock("lk", 30), "异常路径下 lockExecute 也必须释放锁")
    }

    @Test
    fun lockExecuteReturnsNullWhenAcquireFailsAndNoErrorCode() {
        val svc = NormalLockService()
        assertTrue(svc.tryLock("k", 60), "先抢占")
        // 别人已经持有，errorCode=null → 应返回 null 而不是抛
        val result = svc.lockExecute(
            lockKey = "k",
            supplier = { "should-not-run" },
            sec = 1,
            errorCode = null
        )
        assertNull(result)
    }

    // ============================================================
    // 可重入锁 (lock/unLock(lock, key)) 与租约锁 (tryLock) 解耦
    // ============================================================

    @Test
    fun reentrantLockAndLeaseLockAreIndependentMechanisms() {
        // 这条测试钉住"两套机制互不影响"的设计——同一个 key 同时
        // 占用租约锁与可重入锁不冲突，因为它们各自走独立的数据结构
        val svc = NormalLockService()
        assertTrue(svc.tryLock("dual", 60), "拿租约锁")
        val reentrant = svc.lock("dual")
        assertNotNull(reentrant, "再拿可重入锁不应被租约锁挡")
        svc.unLock(reentrant, "dual")
    }

    // ============================================================
    // 并发竞态：多线程同 key tryLock 应有且只有一个成功
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
        assertEquals(1, successCount.get(), "同一 key 在并发下应只有一个线程拿到锁")
    }
}
