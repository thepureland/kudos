package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.ILockProvider
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [DistributedCacheGuardAspect] 集成单测——Spring AOP + `@EnableAspectJAutoProxy`，
 * 不拉起完整 kudos cache 自动配置。
 *
 * 关注点：
 *  - **抢锁成功**：业务方法被调用，proceed 返回结果
 *  - **抢锁失败**：aspect 退避后查一次缓存（仍 miss），仍 proceed——**不会阻塞挂死**（README
 *    的核心契约）
 *  - **lockProvider.tryLock 被调用**：验证锁路径被走过，而非 aspect 整个跳过
 *
 * 测试不依赖 `KeyValueCacheKit` 命中（没注册 `mixCacheManager` MixCacheManager
 * bean，所以 cacheKit 永远 miss），断言围绕"业务方法是否被 invoke"展开。
 *
 * **PER_CLASS 生命周期**：aspect 用 `LockTool.lockProvider`（`by lazy`）一次性捕获
 * lock provider 实例——若每个测试方法都重建 context，第二轮以后的 aspect 仍然指向第一轮
 * 的 lockProvider 引用，导致后续断言看到 stale 计数。改成"context per class"：所有方法
 * 共享一份 lockProvider，[resetState] 在每个方法前清计数器 + 缓存。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DistributedCacheGuardAspectTest {

    private lateinit var ctx: AnnotationConfigApplicationContext
    private lateinit var lockProvider: ControllableLockProvider

    @BeforeAll
    fun classSetup() {
        // 两阶段构造：register → set SpringKit → refresh。aspect 的 companion init 触发
        // `LockTool.lockProvider`（走 `SpringKit.getBeansOfType`），需要 applicationContext
        // 在 refresh **之前**就可达，否则 lazy 字段会一次性失败永久 null。
        ctx = AnnotationConfigApplicationContext()
        ctx.register(TestAopConfig::class.java)
        SpringKit.applicationContext = ctx
        ctx.refresh()
        lockProvider = ctx.getBean(ControllableLockProvider::class.java)
    }

    @AfterAll
    fun classTeardown() {
        ctx.close()
    }

    @BeforeTest
    fun resetState() {
        lockProvider.alwaysTryLockReturns(true)
        ctx.getBean(AtomicInteger::class.java).set(0)
        // 清掉 @Cacheable 的缓存，避免跨测试方法的命中干扰
        val cm = ctx.getBean(ConcurrentMapCacheManager::class.java)
        cm.cacheNames.forEach { cm.getCache(it)?.clear() }
    }

    @Test
    fun lockAcquired_invokesBusinessMethodOnce() {
        lockProvider.alwaysTryLockReturns(true)
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)
        counter.set(0)

        val result = service.loadOnce("a")

        assertEquals("loaded:a", result)
        assertEquals(1, counter.get(), "拿到锁后业务方法应当 invoke 一次")
        assertEquals(1, lockProvider.tryLockCount.get(), "tryLock 应当被调用一次")
        assertEquals(1, lockProvider.unLockCount.get(), "成功路径 finally 应释放一次锁")
    }

    @Test
    fun lockFailed_stillInvokesBusinessMethod_doesNotBlock() {
        lockProvider.alwaysTryLockReturns(false)
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)
        counter.set(0)

        val start = System.currentTimeMillis()
        val result = service.loadOnce("b")
        val elapsed = System.currentTimeMillis() - start

        // 抢锁失败 → 短退避 200ms → 仍 proceed，绝不挂死
        assertEquals("loaded:b", result)
        assertEquals(1, counter.get(), "抢锁失败仍应 proceed 一次，不挂死")
        assert(elapsed < 2000) { "失败路径不应阻塞超过 2s, 实际 ${elapsed}ms" }
        assertEquals(0, lockProvider.unLockCount.get(), "抢锁失败路径不应调 unLock")
    }

    @Test
    fun lockFailed_doesNotRetryLockInfinitely() {
        lockProvider.alwaysTryLockReturns(false)
        val service = ctx.getBean(GuardedService::class.java)

        service.loadOnce("c")

        assertEquals(1, lockProvider.tryLockCount.get(), "抢锁失败后不应循环重试 tryLock")
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableCaching(proxyTargetClass = true)
    open class TestAopConfig {
        @Bean
        open fun cacheManager(): ConcurrentMapCacheManager = ConcurrentMapCacheManager("test-cache")

        @Bean
        open fun lockProvider(): ControllableLockProvider = ControllableLockProvider()

        @Bean
        open fun guardAspect(): DistributedCacheGuardAspect = DistributedCacheGuardAspect()

        @Bean
        open fun callCounter(): AtomicInteger = AtomicInteger(0)

        @Bean
        open fun guardedService(counter: AtomicInteger): GuardedService = GuardedService(counter)
    }

    /** Spring AOP 代理目标类必须 open。计数器外置避免 CGLIB 字段语义干扰。 */
    @Service
    open class GuardedService(val calls: AtomicInteger) {
        @Cacheable("test-cache", key = "#input")
        @DistributedCacheGuard
        open fun loadOnce(input: String): String {
            calls.incrementAndGet()
            return "loaded:$input"
        }
    }

    /** 测试用 ILockProvider：tryLock 返回值可控，并记录调用次数。 */
    open class ControllableLockProvider : ILockProvider<ReentrantLock> {
        val tryLockCount = AtomicInteger(0)
        val unLockCount = AtomicInteger(0)
        @Volatile private var tryLockResult: Boolean = true

        fun alwaysTryLockReturns(value: Boolean) {
            tryLockResult = value
            tryLockCount.set(0)
            unLockCount.set(0)
        }

        override fun tryLock(lockKey: String, sec: Int): Boolean {
            tryLockCount.incrementAndGet()
            return tryLockResult
        }

        override fun unLock(key: String) {
            unLockCount.incrementAndGet()
        }

        override fun lock(key: String): ReentrantLock? = ReentrantLock()
        override fun unLock(lock: Lock, key: String) { /* no-op */ }
        override fun order(): Int = 1 // 比默认 99 优先
    }
}
