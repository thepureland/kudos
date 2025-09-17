package io.kudos.ability.distributed.lock.redisson

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test


/**
 * 分布式锁单结点测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(TestLockByAnnotation::class)
@EnabledIfDockerAvailable
open class RedissonLockSingleTest {

    @Autowired
    private lateinit var testLockByAnnotation: TestLockByAnnotation

    val log = LogFactory.getLog(this)

    /** 锁测试共享变量 */
    private var lockCount = 10

    /** 锁测试共享变量(使用注解加锁的方式) */
    var lockCountByAnnotation = 10

    /** 无锁测试共享变量 */
    private var count = 10

    /** 模拟线程数 */
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
    fun lock() {
        val countDownLatch = CountDownLatch(10)

        for (i in 0 until threadNum) {
            Thread {
                // 无锁操作
                testCount()
                // 加锁操作
                testLockCount()
                // 加锁操作(使用注解加锁的方式)
                testLockByAnnotation.execute(this)
                countDownLatch.countDown()
            }.start()
        }

        countDownLatch.await()
    }

    /**
     * 加锁测试
     */
    private fun testLockCount() {
        val lockKey = "lock-test"
        try {
            // 加锁，设置超时时间2s
            RedissonLockKit.lock(lockKey, TimeUnit.SECONDS, 2)
            lockCount--
            log.info("lockCount: $lockCount")
        } catch (e: Exception) {
            log.error(e, e.message)
        } finally {
            // 释放锁
            RedissonLockKit.unlock(lockKey)
        }
    }

    /**
     * 无锁测试
     */
    private fun testCount() {
        count--
        log.info("count: $count")
    }

}

/**
 * 测试使用注解加锁的方式
 */
open class TestLockByAnnotation {

    @DistributedLock
    fun execute(test: RedissonLockSingleTest) {
        test.lockCountByAnnotation--
        test.log.info("lockCountByAnnotation: ${test.lockCountByAnnotation}")
    }

}