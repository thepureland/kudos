package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.FakeProceedingJoinPoint
import io.kudos.ability.cache.common.aop.methodOf
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.ILockProvider
import org.springframework.cache.Cache
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    private lateinit var kitCacheManager: TestableMixCacheManager

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
        // 给 KeyValueCacheKit 注入内存版 MixCacheManager：缺省各缓存为空（保持原“总是 miss”的行为），
        // 命中类用例往里预放值即可。
        kitCacheManager = TestableMixCacheManager()
        KeyValueCacheKit.overrideForTesting(cacheManager = kitCacheManager)
    }

    @AfterAll
    fun classTeardown() {
        KeyValueCacheKit.resetForTesting()
        KudosContextHolder.clear()
        ctx.close()
    }

    @BeforeTest
    fun resetState() {
        lockProvider.alwaysTryLockReturns(true)
        lockProvider.onTryLock = null
        lockProvider.throwOnUnlock = false
        ctx.getBean(AtomicInteger::class.java).set(0)
        // Clear the @Cacheable cache to avoid cross-method hit contamination.
        val cm = ctx.getBean(ConcurrentMapCacheManager::class.java)
        cm.cacheNames.forEach { cm.getCache(it)?.clear() }
        kitCacheManager.clearAll()
        KudosContextHolder.get().tenantId = "T1"
    }

    @AfterTest
    fun cleanInterruptFlag() {
        // 防止个别用例残留中断标志影响后续测试
        Thread.interrupted()
    }

    @Test
    fun pointcut_isInvocable() {
        // 切点方法本身是空体声明，Spring AOP 运行时不会执行它；直接调用以确认可达
        ctx.getBean(DistributedCacheGuardAspect::class.java).cut()
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

    // ---- 缓存命中类分支（围绕 KeyValueCacheKit 三处 getValue）---------------

    @Test
    fun preLockCacheHit_returnsCachedWithoutLocking() {
        kitCacheManager.getOrCreate("test-cache").put("hit", "cached-v")
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)

        val result = service.loadOnce("hit")

        assertEquals("cached-v", result, "锁前命中应直接返回缓存值")
        assertEquals(0, counter.get(), "命中后不应执行业务方法")
        assertEquals(0, lockProvider.tryLockCount.get(), "命中后不应竞争锁")
    }

    @Test
    fun preLockCachedNullIsAHit_doesNotLockOrProceed() {
        // 负缓存：源端确实没有这条数据时缓存 null。判定命中必须看"条目是否存在"，而不是"值是否非空"——
        // 后者会让每次调用都重新加锁回源，正好是防穿透要挡住的场景。
        kitCacheManager.getOrCreate("test-cache").put("nullkey", null)
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)

        val result = service.loadOnce("nullkey")

        assertNull(result, "命中的 null 应原样返回")
        assertEquals(0, counter.get(), "命中 null 后不应再执行业务方法")
        assertEquals(0, lockProvider.tryLockCount.get(), "命中 null 后不应竞争分布式锁")
    }

    @Test
    fun lockFailed_cacheFilledDuringBackoff_returnsCachedWithoutProceed() {
        // tryLock 返回 false 的同时往缓存写值，模拟“别的线程在退避期间完成了装载”
        lockProvider.alwaysTryLockReturns(false)
        lockProvider.onTryLock = { kitCacheManager.getOrCreate("test-cache").put("bf", "other-thread-v") }
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)

        val result = service.loadOnce("bf")

        assertEquals("other-thread-v", result, "退避后二次查缓存命中应复用他人结果")
        assertEquals(0, counter.get(), "复用他人结果时不应再执行业务方法")
    }

    @Test
    fun lockAcquired_doubleCheckHit_returnsCachedAndUnlocks() {
        // tryLock 成功的同时往缓存写值，模拟“拿到锁后双重检查命中”
        lockProvider.onTryLock = { kitCacheManager.getOrCreate("test-cache").put("dc", "filled-v") }
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)

        val result = service.loadOnce("dc")

        assertEquals("filled-v", result, "锁内双重检查命中应直接返回")
        assertEquals(0, counter.get())
        assertEquals(1, lockProvider.unLockCount.get(), "锁内命中也必须释放锁")
    }

    @Test
    fun unlockFailure_isSwallowed_resultStillReturned() {
        lockProvider.throwOnUnlock = true
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)

        val result = service.loadOnce("uf")

        assertEquals("loaded:uf", result, "释放锁失败只应告警，不应影响业务返回")
        assertEquals(1, counter.get())
    }

    @Test
    fun interruptedDuringBackoff_restoresInterruptFlag_andStillProceeds() {
        lockProvider.alwaysTryLockReturns(false)
        val service = ctx.getBean(GuardedService::class.java)
        val counter = ctx.getBean(AtomicInteger::class.java)

        Thread.currentThread().interrupt()
        val result = service.loadOnce("intr")

        assertTrue(Thread.interrupted(), "InterruptedException 后必须恢复中断标志")
        assertEquals("loaded:intr", result)
        assertEquals(1, counter.get(), "中断后仍应走原方法，不能吞掉结果")
    }

    // ---- @TenantCacheable 路径 ----------------------------------------------
    // 这里用假 pjp 直接调 around 来覆盖租户分支：本测试只关心 DistributedCacheGuardAspect 自身如何从注解
    // 解析出 (cacheName, cacheKey)，绕开代理可以不必搭一整套 Spring Cache 基础设施。
    // （历史原因已消失：@TenantCacheable 曾因缺少 @Cacheable 元标注而在 Spring 内省时抛
    //   AnnotationConfigurationException，现已修复，契约由 TenantCacheAnnotationsTest 保证。）

    @Test
    fun tenantCacheable_buildsLockKeyWithTenantPrefix() {
        val method = methodOf(AnnotatedHolder::class.java, "tenantValueOnly")
        val pjp = FakeProceedingJoinPoint(method, arrayOf("a"), AnnotatedHolder()) { "tenant:a" }

        val result = ctx.getBean(DistributedCacheGuardAspect::class.java).around(pjp)

        assertEquals("tenant:a", result)
        assertEquals(1, pjp.proceedCount)
        assertEquals(listOf("lock:tenant-cache:T1::a"), lockProvider.lockKeys,
            "TenantCacheable 路径锁 key 应包含租户前缀")
    }

    @Test
    fun tenantCacheable_cacheNamesAttribute_isAcceptedAsFallback() {
        val method = methodOf(AnnotatedHolder::class.java, "tenantNamesOnly")
        val pjp = FakeProceedingJoinPoint(method, arrayOf("z"), AnnotatedHolder()) { "tenant-proceeded" }

        val result = ctx.getBean(DistributedCacheGuardAspect::class.java).around(pjp)

        assertEquals("tenant-proceeded", result)
        assertTrue(lockProvider.lockKeys.single().startsWith("lock:tc2:T1::z"),
            "应回退使用 cacheNames 属性，实际: ${lockProvider.lockKeys}")
    }

    // ---- getCachePair 的异常分支（直接调 around，无需代理）-------------------

    @Test
    fun missingBothAnnotations_throwsIllegalState() {
        val e = assertFailsWith<IllegalStateException> { aroundOn("plain") }
        assertTrue(e.message!!.contains("@Cacheable or @TenantCacheable"), "实际消息: ${e.message}")
    }

    @Test
    fun cacheableWithoutNames_throwsIllegalState() {
        val e = assertFailsWith<IllegalStateException> { aroundOn("noNames") }
        assertTrue(e.message!!.contains("must specify value or cacheNames"), "实际消息: ${e.message}")
    }

    @Test
    fun cacheableWithoutKey_throwsIllegalArgument() {
        val e = assertFailsWith<IllegalArgumentException> { aroundOn("noKey") }
        assertTrue(e.message!!.contains("@Cacheable.key must be specified"), "实际消息: ${e.message}")
    }

    @Test
    fun cacheableKeyResolvesNull_throwsIllegalState() {
        val e = assertFailsWith<IllegalStateException> { aroundOn("nullSpelKey") }
        assertTrue(e.message!!.contains("resolved to empty"), "实际消息: ${e.message}")
    }

    @Test
    fun cacheableCacheNamesAttribute_isAcceptedAsFallback() {
        val result = aroundOn("namesOnly")
        assertEquals("proceeded", result, "只声明 cacheNames（不写 value）也应被接受")
        assertEquals(listOf("lock:c2:a"), lockProvider.lockKeys)
    }

    @Test
    fun tenantCacheableWithoutNames_throwsIllegalState() {
        val e = assertFailsWith<IllegalStateException> { aroundOn("tenantNoNames") }
        assertTrue(e.message!!.contains("must specify value or cacheNames"), "实际消息: ${e.message}")
    }

    private fun aroundOn(methodName: String): Any? {
        val method = methodOf(AnnotatedHolder::class.java, methodName)
        val pjp = FakeProceedingJoinPoint(method, arrayOf("a"), AnnotatedHolder()) { "proceeded" }
        return ctx.getBean(DistributedCacheGuardAspect::class.java).around(pjp)
    }

    /** getCachePair 各分支的注解持有类：方法体不会被执行（proceed 由假 pjp 接管）。 */
    @Suppress("unused", "UNUSED_PARAMETER")
    open class AnnotatedHolder {
        fun plain(input: String): String = "x"

        @Cacheable(key = "#input")
        fun noNames(input: String): String = "x"

        @Cacheable(value = ["c"])
        fun noKey(input: String): String = "x"

        @Cacheable(value = ["c"], key = "#missing")
        fun nullSpelKey(input: String): String = "x"

        @Cacheable(cacheNames = ["c2"], key = "#input")
        fun namesOnly(input: String): String = "x"

        @TenantCacheable
        fun tenantNoNames(input: String): String = "x"

        @TenantCacheable(cacheNames = ["tc2"], suffix = "#input")
        fun tenantNamesOnly(input: String): String = "x"

        @TenantCacheable(value = ["tenant-cache"], suffix = "#input")
        fun tenantValueOnly(input: String): String = "x"
    }

    /** 内存版 [MixCacheManager]：getCache 直接按名创建 ConcurrentMapCache，绕过版本前缀逻辑。 */
    class TestableMixCacheManager : MixCacheManager() {
        private val caches = ConcurrentHashMap<String, Cache>()
        fun getOrCreate(name: String): Cache = caches.computeIfAbsent(name) { ConcurrentMapCache(it, true) }
        fun clearAll() = caches.values.forEach { it.clear() }
        override fun getCache(name: String): Cache = getOrCreate(name)
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

        @Bean
        open fun tenantCacheKeyGenerator(): TenantCacheKeyGenerator = TenantCacheKeyGenerator()
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
        /** 记录每次 tryLock 收到的锁 key，便于断言锁键格式。 */
        val lockKeys = mutableListOf<String>()
        /** tryLock 时的副作用钩子（模拟“竞争期间他人写入缓存”）。 */
        @Volatile var onTryLock: (() -> Unit)? = null
        /** unLock 是否抛异常（验证释放失败被吞掉的路径）。 */
        @Volatile var throwOnUnlock: Boolean = false
        @Volatile private var tryLockResult: Boolean = true

        fun alwaysTryLockReturns(value: Boolean) {
            tryLockResult = value
            tryLockCount.set(0)
            unLockCount.set(0)
            lockKeys.clear()
        }

        override fun tryLock(lockKey: String, sec: Int): Boolean {
            tryLockCount.incrementAndGet()
            lockKeys.add(lockKey)
            onTryLock?.invoke()
            return tryLockResult
        }

        override fun unLock(key: String) {
            unLockCount.incrementAndGet()
            if (throwOnUnlock) throw IllegalStateException("unlock boom")
        }

        override fun lock(key: String): ReentrantLock? = ReentrantLock()
        override fun unLock(lock: Lock, key: String) { /* no-op */ }
        override fun order(): Int = 1 // higher priority than the default 99
    }
}
