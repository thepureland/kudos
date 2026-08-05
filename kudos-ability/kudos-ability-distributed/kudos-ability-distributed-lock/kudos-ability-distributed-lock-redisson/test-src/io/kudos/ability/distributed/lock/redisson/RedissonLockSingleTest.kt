package io.kudos.ability.distributed.lock.redisson

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test


/**
 * Single-node distributed lock test case.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(TestLockByAnnotation::class)
@EnabledIfDockerInstalled
open class RedissonLockSingleTest {

    @Autowired
    private lateinit var testLockByAnnotation: TestLockByAnnotation

    val log = LogFactory.getLog(this::class)

    /** Shared variable for the locked test. */
    private var lockCount = 10

    /** Shared variable for the annotation-based locked test. */
    var lockCountByAnnotation = 10

    /** Shared variable for the unlocked test. */
    private var count = 10

    /** Number of simulated threads. */
    private val threadNum = 10

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            val container = RedisTestContainer.startIfNeeded(registry)
            registry.add("kudos.ability.distributed.lock.redisson.singleServerConfig.address") {
                "redis://127.0.0.1:${container.ports.first().publicPort}"
            }
        }
    }

    @Test
    fun redissonClientBeanIsAccessibleFromKit() {
        val client = RedissonLockKit.redissonClient()
        kotlin.test.assertNotNull(client)
        kotlin.test.assertFalse(client.isShutdown)
    }

    @Test
    fun lock() {
        val countDownLatch = CountDownLatch(threadNum)

        for (i in 0 until threadNum) {
            Thread {
                try {
                    // Lock-free operation
                    testCount()
                    // Lock-based operation
                    testLockCount()
                    // Lock-based operation (annotation style)
                    testLockByAnnotation.execute(this)
                } finally {
                    // Always count down so an unexpected failure surfaces via the assertions below instead of hanging await().
                    countDownLatch.countDown()
                }
            }.start()
        }

        kotlin.test.assertTrue(
            countDownLatch.await(60, TimeUnit.SECONDS),
            "all $threadNum worker threads should finish within the timeout"
        )
        // Each lock-protected counter is decremented exactly once per thread, so both must end at 0.
        kotlin.test.assertEquals(0, lockCount, "lock-API protected counter must reach 0")
        kotlin.test.assertEquals(0, lockCountByAnnotation, "annotation protected counter must reach 0")
    }

    /**
     * Lock-based test.
     */
    private fun testLockCount() {
        val lockKey = "lock-test"
        try {
            // Acquire lock with a 2s timeout
            RedissonLockKit.lock(lockKey, TimeUnit.SECONDS, 2)
            lockCount--
            log.info("lockCount: $lockCount")
        } catch (e: Exception) {
            log.error(e, e.message)
        } finally {
            // Release the lock
            RedissonLockKit.unlock(lockKey)
        }
    }

    /**
     * Lock-free test.
     */
    private fun testCount() {
        count--
        log.info("count: $count")
    }

}

/**
 * Tests the annotation-based locking style.
 */
open class TestLockByAnnotation {

    // waitTime > 0 so the 10 contending threads queue deterministically instead of failing on a single
    // non-blocking attempt (the annotation default waitTime = 0 throws DistributedLockAcquireException on contention).
    @DistributedLock(waitTime = 30)
    fun execute(test: RedissonLockSingleTest) {
        test.lockCountByAnnotation--
        test.log.info("lockCountByAnnotation: ${test.lockCountByAnnotation}")
    }

}