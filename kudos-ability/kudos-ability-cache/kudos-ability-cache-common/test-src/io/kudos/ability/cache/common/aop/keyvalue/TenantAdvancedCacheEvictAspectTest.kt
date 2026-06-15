package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.context.core.KudosContextHolder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [TenantAdvancedCacheEvictAspect] 单元测试。
 *
 * 验证：
 *  - 方法正常返回后按 "cacheKey::tenantId" 调用 clearCache（携带 dataKey 与 allEntries）
 *  - allEntries=true 的参数透传
 *  - dataKey 缺省为空字符串
 *  - 方法抛异常时（@After 语义）仍会清缓存，且异常原样上抛
 *  - 未注入 IRemoteCacheProcessor 时切面为 no-op（单独上下文验证）
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TenantAdvancedCacheEvictAspectTest {

    private lateinit var ctx: AnnotationConfigApplicationContext
    private lateinit var processor: RecordingRemoteCacheProcessor
    private lateinit var calls: AtomicInteger

    @BeforeAll
    fun classSetup() {
        ctx = AnnotationConfigApplicationContext(TestConfig::class.java)
        processor = ctx.getBean(RecordingRemoteCacheProcessor::class.java)
        calls = ctx.getBean(AtomicInteger::class.java)
    }

    @AfterAll
    fun classTeardown() {
        KudosContextHolder.clear()
        ctx.close()
    }

    @BeforeTest
    fun reset() {
        processor.reset()
        calls.set(0)
        KudosContextHolder.get().tenantId = "T9"
    }

    @Test
    fun pointcut_isInvocable() {
        // 切点方法本身是空体声明，Spring AOP 运行时不会执行它；直接调用以确认可达
        ctx.getBean(TenantAdvancedCacheEvictAspect::class.java).cut()
    }

    @Test
    fun afterMethod_clearsCacheWithTenantKey() {
        val service = ctx.getBean(EvictingService::class.java)

        assertEquals("done", service.remove())

        assertEquals(1, calls.get())
        assertEquals(Triple("evKey::T9", "dk", false), processor.clears.single(),
            "应在方法返回后按 cacheKey::tenantId 清缓存")
    }

    @Test
    fun allEntriesTrue_isPassedThrough() {
        val service = ctx.getBean(EvictingService::class.java)

        service.removeAll()

        assertEquals(Triple("evKey::T9", "", true), processor.clears.single(),
            "allEntries 与缺省 dataKey 应原样透传")
    }

    @Test
    fun methodThrows_cacheStillCleared_exceptionPropagated() {
        val service = ctx.getBean(EvictingService::class.java)

        val e = assertFailsWith<IllegalStateException> { service.removeBoom() }
        assertEquals("boom", e.message)
        assertEquals(Triple("evKey::T9", "dk", false), processor.clears.single(),
            "@After 语义：方法抛异常也应清缓存")
    }

    @Test
    fun missingProcessor_isNoOp() {
        val ctx2 = AnnotationConfigApplicationContext(NoProcessorConfig::class.java)
        try {
            val calls2 = ctx2.getBean(AtomicInteger::class.java)
            val service = ctx2.getBean(EvictingService::class.java)

            assertEquals("done", service.remove())
            assertEquals(1, calls2.get(), "无远端处理器时业务方法应正常执行")
        } finally {
            ctx2.close()
        }
    }

    // ---- 测试装配 ------------------------------------------------------------

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    open class TestConfig {
        @Bean
        open fun aspect(): TenantAdvancedCacheEvictAspect = TenantAdvancedCacheEvictAspect()

        @Bean
        open fun processor(): RecordingRemoteCacheProcessor = RecordingRemoteCacheProcessor()

        @Bean
        open fun calls(): AtomicInteger = AtomicInteger(0)

        @Bean
        open fun evictingService(calls: AtomicInteger): EvictingService = EvictingService(calls)
    }

    /** 无 IRemoteCacheProcessor 的上下文（验证 no-op）。 */
    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    open class NoProcessorConfig {
        @Bean
        open fun aspect(): TenantAdvancedCacheEvictAspect = TenantAdvancedCacheEvictAspect()

        @Bean
        open fun calls(): AtomicInteger = AtomicInteger(0)

        @Bean
        open fun evictingService(calls: AtomicInteger): EvictingService = EvictingService(calls)
    }

    @Service
    open class EvictingService(val calls: AtomicInteger) {

        @TenantAdvancedCacheEvict(cacheKey = "evKey", dataKey = "dk")
        open fun remove(): String {
            calls.incrementAndGet()
            return "done"
        }

        @TenantAdvancedCacheEvict(cacheKey = "evKey", allEntries = true)
        open fun removeAll(): String {
            calls.incrementAndGet()
            return "done"
        }

        @TenantAdvancedCacheEvict(cacheKey = "evKey", dataKey = "dk")
        open fun removeBoom(): String {
            calls.incrementAndGet()
            throw IllegalStateException("boom")
        }
    }
}
