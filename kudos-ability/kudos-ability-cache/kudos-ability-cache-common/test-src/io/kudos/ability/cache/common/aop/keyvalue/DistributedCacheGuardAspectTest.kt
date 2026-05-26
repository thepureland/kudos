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
 * Integration tests for [DistributedCacheGuardAspect] — Spring AOP + `@EnableAspectJAutoProxy`,
 * without bringing up the full kudos cache auto-configuration.
 *
 * Focus:
 *  - **Lock acquired**: business method is invoked, proceed returns the result.
 *  - **Lock failed**: the aspect backs off, queries the cache once (still misses), and proceeds anyway —
 *    **must not block indefinitely** (the README's core contract).
 *  - **lockProvider.tryLock is called**: verifies the lock path was exercised and the aspect didn't skip entirely.
 *
 * The tests do not rely on `KeyValueCacheKit` cache hits (no `mixCacheManager` MixCacheManager bean is registered,
 * so cacheKit always misses); assertions revolve around "was the business method invoked".
 *
 * **PER_CLASS lifecycle**: the aspect captures the lock provider instance once via `LockTool.lockProvider`
 * (`by lazy`) — if the context were rebuilt per test method, the aspect after the first round would still
 * point to the first round's lockProvider reference, and later assertions would see stale counters. The
 * "context per class" setup is used instead: all methods share a single lockProvider, and [resetState] clears
 * the counters + cache before each method.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DistributedCacheGuardAspectTest {

    private lateinit var ctx: AnnotationConfigApplicationContext
    private lateinit var lockProvider: ControllableLockProvider

    @BeforeAll
    fun classSetup() {
        // Two-phase construction: register → set SpringKit → refresh. The aspect's companion init triggers
        // `LockTool.lockProvider` (which goes through `SpringKit.getBeansOfType`), so the applicationContext
        // must be reachable **before** refresh; otherwise the lazy field fails once and stays null forever.
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
        // Clear the @Cacheable cache to avoid cross-method hit contamination.
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
        assertEquals(1, counter.get(), "Business method should be invoked once after the lock is acquired")
        assertEquals(1, lockProvider.tryLockCount.get(), "tryLock should be called once")
        assertEquals(1, lockProvider.unLockCount.get(), "The success path's finally should release the lock once")
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

        // Lock acquisition fails → short 200ms backoff → still proceed, never hangs.
        assertEquals("loaded:b", result)
        assertEquals(1, counter.get(), "Lock failure should still proceed once and not hang")
        assert(elapsed < 2000) { "Failure path must not block longer than 2s; actual ${elapsed}ms" }
        assertEquals(0, lockProvider.unLockCount.get(), "Lock-failure path must not call unLock")
    }

    @Test
    fun lockFailed_doesNotRetryLockInfinitely() {
        lockProvider.alwaysTryLockReturns(false)
        val service = ctx.getBean(GuardedService::class.java)

        service.loadOnce("c")

        assertEquals(1, lockProvider.tryLockCount.get(), "After lock failure, tryLock must not be retried in a loop")
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

    /** A Spring AOP proxy target class must be open. The counter is external to avoid CGLIB field-semantics interference. */
    @Service
    open class GuardedService(val calls: AtomicInteger) {
        @Cacheable("test-cache", key = "#input")
        @DistributedCacheGuard
        open fun loadOnce(input: String): String {
            calls.incrementAndGet()
            return "loaded:$input"
        }
    }

    /** Test ILockProvider: tryLock return value is controllable, and call counts are recorded. */
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
        override fun order(): Int = 1 // higher priority than the default 99
    }
}
