package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [TenantAdvancedCacheableAspect] 单元测试。
 *
 * 通过 Spring AOP 代理 + 内存版 [IRemoteCacheProcessor] 验证：
 *  - 缓存命中：直接返回远端值，不执行业务方法、不回写
 *  - 缓存未命中 + 非空结果：执行业务方法并以 "cacheKey::tenantId" 回写（携带 dataKey 与 timeOut）
 *  - 缓存未命中 + null 结果：不回写（避免缓存无意义空占位）
 *  - timeOut 缺省值 1800000 毫秒
 *  - tenantId 为 null 时 key 拼为 "cacheKey::null"
 *  - 未注入 IRemoteCacheProcessor 时切面降级为直通（单独上下文验证）
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TenantAdvancedCacheableAspectTest {

    private lateinit var ctx: AnnotationConfigApplicationContext
    private lateinit var processor: RecordingRemoteCacheProcessor
    private lateinit var payload: PayloadHolder
    private lateinit var calls: AtomicInteger

    @BeforeAll
    fun classSetup() {
        ctx = AnnotationConfigApplicationContext(TestConfig::class.java)
        processor = ctx.getBean(RecordingRemoteCacheProcessor::class.java)
        payload = ctx.getBean(PayloadHolder::class.java)
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
        payload.value = "loaded"
        calls.set(0)
        KudosContextHolder.get().tenantId = "T9"
    }

    @Test
    fun pointcut_isInvocable() {
        // 切点方法本身是空体声明，Spring AOP 运行时不会执行它；直接调用以确认可达
        ctx.getBean(TenantAdvancedCacheableAspect::class.java).cut()
    }

    @Test
    fun cacheMiss_invokesMethod_andWritesBackWithTenantKeyAndTimeout() {
        val service = ctx.getBean(AdvancedService::class.java)

        val result = service.load()

        assertEquals("loaded", result)
        assertEquals(1, calls.get())
        val write = processor.writes.single()
        assertEquals("advKey::T9", write.cacheKey, "缓存 key 应自动拼接租户 id")
        assertEquals("dk", write.dataKey)
        assertEquals("loaded", write.value)
        assertEquals(5000L, write.timeOut)
    }

    @Test
    fun cacheHit_returnsRemoteValue_withoutInvokingMethod() {
        processor.data["advKey::T9" to "dk"] = "remote-v"
        val service = ctx.getBean(AdvancedService::class.java)

        val result = service.load()

        assertEquals("remote-v", result)
        assertEquals(0, calls.get(), "命中后不应执行业务方法")
        assertTrue(processor.writes.isEmpty(), "命中后不应回写")
    }

    @Test
    fun nullResult_isNotWrittenBack() {
        payload.value = null
        val service = ctx.getBean(AdvancedService::class.java)

        assertNull(service.load())
        assertEquals(1, calls.get())
        assertTrue(processor.writes.isEmpty(), "null 结果不应回写缓存")
    }

    @Test
    fun defaultTimeout_is30Minutes() {
        val service = ctx.getBean(AdvancedService::class.java)

        service.loadWithDefaults()

        assertEquals(1800000L, processor.writes.single().timeOut, "未指定 timeOut 时应为默认 1800000 毫秒")
    }

    @Test
    fun nullTenantId_keyContainsLiteralNull() {
        KudosContextHolder.get().tenantId = null
        val service = ctx.getBean(AdvancedService::class.java)

        service.load()

        assertEquals("advKey::null", processor.writes.single().cacheKey)
    }

    @Test
    fun missingProcessor_degradesToPassThrough() {
        val ctx2 = AnnotationConfigApplicationContext(NoProcessorConfig::class.java)
        try {
            val calls2 = ctx2.getBean(AtomicInteger::class.java)
            val service = ctx2.getBean(AdvancedService::class.java)

            assertEquals("loaded", service.load())
            assertEquals(1, calls2.get(), "无远端处理器时应直通业务方法")
        } finally {
            ctx2.close()
        }
    }

    // ---- 测试装配 ------------------------------------------------------------

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    open class TestConfig {
        @Bean
        open fun aspect(): TenantAdvancedCacheableAspect = TenantAdvancedCacheableAspect()

        @Bean
        open fun processor(): RecordingRemoteCacheProcessor = RecordingRemoteCacheProcessor()

        @Bean
        open fun calls(): AtomicInteger = AtomicInteger(0)

        @Bean
        open fun payload(): PayloadHolder = PayloadHolder()

        @Bean
        open fun advancedService(calls: AtomicInteger, payload: PayloadHolder): AdvancedService =
            AdvancedService(calls, payload)
    }

    /** 无 IRemoteCacheProcessor 的上下文（验证降级直通）。 */
    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    open class NoProcessorConfig {
        @Bean
        open fun aspect(): TenantAdvancedCacheableAspect = TenantAdvancedCacheableAspect()

        @Bean
        open fun calls(): AtomicInteger = AtomicInteger(0)

        @Bean
        open fun payload(): PayloadHolder = PayloadHolder()

        @Bean
        open fun advancedService(calls: AtomicInteger, payload: PayloadHolder): AdvancedService =
            AdvancedService(calls, payload)
    }

    /** 外置可变返回值容器（CGLIB 代理目标的类内字段语义不可靠）。 */
    open class PayloadHolder {
        @Volatile var value: String? = "loaded"
    }

    @Service
    open class AdvancedService(val calls: AtomicInteger, val payload: PayloadHolder) {

        @TenantAdvancedCacheable(cacheKey = "advKey", dataKey = "dk", timeOut = 5000)
        open fun load(): String? {
            calls.incrementAndGet()
            return payload.value
        }

        @TenantAdvancedCacheable(cacheKey = "advKey2", dataKey = "dk2")
        open fun loadWithDefaults(): String? {
            calls.incrementAndGet()
            return payload.value
        }
    }
}

/** 记录读写的内存版 [IRemoteCacheProcessor]。 */
internal class RecordingRemoteCacheProcessor : IRemoteCacheProcessor {

    data class Write(val cacheKey: String, val dataKey: String, val value: Any?, val timeOut: Long)

    val data = mutableMapOf<Pair<String, String>, Any?>()
    val writes = mutableListOf<Write>()
    val clears = mutableListOf<Triple<String, String, Boolean>>()

    fun reset() {
        data.clear()
        writes.clear()
        clears.clear()
    }

    override fun getCacheData(cacheKey: String, dataKey: String): Any? = data[cacheKey to dataKey]

    override fun writeCacheData(cacheKey: String, dataKey: String, o: Any?, timeOut: Long) {
        writes += Write(cacheKey, dataKey, o, timeOut)
        data[cacheKey to dataKey] = o
    }

    override fun clearCache(cacheKey: String, s: String, b: Boolean) {
        clears += Triple(cacheKey, s, b)
    }
}
