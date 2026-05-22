package io.kudos.ability.distributed.client.feign.interceptor

import feign.Request
import feign.RequestTemplate
import io.kudos.ability.distributed.client.feign.init.properties.OpenFeignProperties
import io.kudos.ability.distributed.client.feign.support.IFeignRequestContextProcess
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import org.springframework.core.Ordered
import org.springframework.context.support.StaticApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [GlobalHeaderRequestInterceptor] 请求头注入单测。
 *
 * 覆盖：
 *  - **基础上下文头**：tenantId / subSysCode / dataSourceId / locale 都按
 *    [Consts.RequestHeader] 约定写入
 *  - **traceKey 缺失时生成 UUID 并反写回 [KudosContext]** ——同进程后续 Feign 调用
 *    共享同一 traceKey（修复 round-6 bug：旧实现只生成不反写）
 *  - **traceKey 已存在时原样使用**，不被覆盖
 *  - **dataSourceId 缺失**时不写对应 header（可选字段）
 *  - **locale 缺失**时回退 `zh_CN`
 *  - **FEIGN_REQUEST 标记**始终写
 *  - **`IFeignRequestContextProcess` SPI 被调用**——验证 bean 缓存路径工作
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class GlobalHeaderRequestInterceptorTest {

    private lateinit var ctx: StaticApplicationContext
    private lateinit var interceptor: GlobalHeaderRequestInterceptor

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        interceptor = GlobalHeaderRequestInterceptor()
        // 每个测试独立的上下文
        KudosContextHolder.clear()
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
        ctx.close()
    }

    @Test
    fun writesAllStandardHeaders_whenContextPopulated() {
        val context = KudosContextHolder.get().apply {
            tenantId = "tenant-X"
            subSystemCode = "sys-A"
            traceKey = "trace-existing"
            dataSourceId = "ds-1"
            clientInfo = ClientInfo(ClientInfo.Builder()).apply { locale = Locale.US }
        }

        val template = newTemplate("GET", "/users/1")
        interceptor.apply(template)

        assertEquals(listOf("tenant-X"), template.headers()[Consts.RequestHeader.TENANT_ID]?.toList())
        assertEquals(listOf("sys-A"), template.headers()[Consts.RequestHeader.SUB_SYS_CODE]?.toList())
        assertEquals(listOf("trace-existing"), template.headers()[Consts.RequestHeader.TRACE_KEY]?.toList())
        assertEquals(listOf("ds-1"), template.headers()[Consts.RequestHeader.DATASOURCE_ID]?.toList())
        assertEquals(listOf("en_US"), template.headers()[Consts.RequestHeader.LOCAL]?.toList())
        assertEquals(listOf("true"), template.headers()[Consts.RequestHeader.FEIGN_REQUEST]?.toList())
        // 已有 traceKey 应被保留
        assertEquals("trace-existing", context.traceKey)
    }

    @Test
    fun missingTraceKey_generatesUuid_andWritesBackToContext() {
        val context = KudosContextHolder.get()
        context.tenantId = "t"
        assertNull(context.traceKey)

        val template = newTemplate("GET", "/x")
        interceptor.apply(template)

        val generated = template.headers()[Consts.RequestHeader.TRACE_KEY]?.firstOrNull()
        assertNotNull(generated)
        assertTrue(generated.isNotBlank(), "应当生成非空 UUID")

        // 关键断言：生成的 UUID 已反写回 context.traceKey
        assertEquals(generated, context.traceKey,
            "traceKey 缺失时生成的 UUID 应当反写回 KudosContext，使后续出站调用复用同一 traceKey")
    }

    @Test
    fun secondCall_reusesGeneratedTraceKey_fromContext() {
        val context = KudosContextHolder.get()
        context.tenantId = "t"

        val t1 = newTemplate("GET", "/a")
        interceptor.apply(t1)
        val firstTrace = t1.headers()[Consts.RequestHeader.TRACE_KEY]!!.single()

        val t2 = newTemplate("GET", "/b")
        interceptor.apply(t2)
        val secondTrace = t2.headers()[Consts.RequestHeader.TRACE_KEY]!!.single()

        assertEquals(firstTrace, secondTrace, "同一线程内后续 Feign 调用应复用第一次生成的 UUID")
    }

    @Test
    fun blankTraceKey_treatedAsAbsent_generatesAndWritesBack() {
        val context = KudosContextHolder.get()
        context.traceKey = "   "

        val template = newTemplate("GET", "/x")
        interceptor.apply(template)

        val generated = template.headers()[Consts.RequestHeader.TRACE_KEY]?.firstOrNull()
        assertNotNull(generated)
        assertTrue(generated.isNotBlank())
        // 空白字符串被视为缺失，覆盖回 context
        assertEquals(generated, context.traceKey)
    }

    @Test
    fun missingDataSourceId_skipsHeader() {
        KudosContextHolder.get().apply {
            tenantId = "t"
            dataSourceId = null
        }
        val template = newTemplate("GET", "/x")
        interceptor.apply(template)
        assertNull(template.headers()[Consts.RequestHeader.DATASOURCE_ID],
            "dataSourceId 为 null 时不应写对应 header")
    }

    @Test
    fun missingLocale_defaultsToZhCN() {
        val template = newTemplate("GET", "/x")
        interceptor.apply(template)
        assertEquals(listOf("zh_CN"), template.headers()[Consts.RequestHeader.LOCAL]?.toList())
    }

    @Test
    fun contextSignatureSecretBlank_doesNotWriteSignatureHeaders() {
        val template = newTemplate("GET", "/x")

        GlobalHeaderRequestInterceptor(OpenFeignProperties()).apply(template)

        assertNull(template.headers()[FeignContextSignature.TIMESTAMP_HEADER])
        assertNull(template.headers()[FeignContextSignature.NONCE_HEADER])
        assertNull(template.headers()[FeignContextSignature.SIGNATURE_HEADER])
    }

    @Test
    fun contextSignatureSecretPresent_writesStableHmacSignatureHeaders() {
        KudosContextHolder.get().apply {
            tenantId = "tenant-X"
            subSystemCode = "sys-A"
            traceKey = "trace-existing"
            dataSourceId = "ds-1"
            clientInfo = ClientInfo(ClientInfo.Builder()).apply { locale = Locale.US }
        }
        val properties = OpenFeignProperties().apply {
            contextSignatureSecret = "secret"
        }
        val signedInterceptor = GlobalHeaderRequestInterceptor(
            properties = properties,
            clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneOffset.UTC),
            nonceSupplier = { "nonce-1" }
        )
        val template = newTemplate("POST", "/users")

        signedInterceptor.apply(template)

        assertEquals(listOf("123456789"), template.headers()[FeignContextSignature.TIMESTAMP_HEADER]?.toList())
        assertEquals(listOf("nonce-1"), template.headers()[FeignContextSignature.NONCE_HEADER]?.toList())
        assertEquals(
            listOf("ThwD6SWfxwc+u9IRzOhYhTic8ZQqh7ZdzOdcZU2CN2s="),
            template.headers()[FeignContextSignature.SIGNATURE_HEADER]?.toList()
        )
    }

    @Test
    fun extensionProcessorIsInvoked_andCached() {
        // 注册一个 SPI 实现，调用 apply 两次——验证 lazy 缓存路径不丢调用
        val processor = RecordingProcessor()
        ctx.beanFactory.registerSingleton("recordingProcessor", processor)

        val interceptor = GlobalHeaderRequestInterceptor()
        val t1 = newTemplate("GET", "/a")
        interceptor.apply(t1)
        val t2 = newTemplate("GET", "/b")
        interceptor.apply(t2)

        assertEquals(2, processor.calls, "processor 应当被每次 apply 调用一次")
        // 第二次调用走 lazy 缓存而非 SpringKit 反射——但应当还是同一 processor 实例
        assertSame(processor, processor)
    }

    @Test
    fun extensionProcessorsAreAppliedInSpringOrder() {
        val calls = mutableListOf<String>()
        ctx.beanFactory.registerSingleton("laterProcessor", OrderedRecordingProcessor("later", 20, calls))
        ctx.beanFactory.registerSingleton("earlierProcessor", OrderedRecordingProcessor("earlier", 10, calls))

        val interceptor = GlobalHeaderRequestInterceptor()
        interceptor.apply(newTemplate("GET", "/ordered"))

        assertEquals(listOf("earlier", "later"), calls)
    }

    private fun newTemplate(method: String, url: String): RequestTemplate =
        RequestTemplate().apply {
            method(Request.HttpMethod.valueOf(method))
            uri(url)
            target("http://localhost")
        }

    /**
     * 记录请求上下文处理器调用次数的测试实现。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingProcessor : IFeignRequestContextProcess {
        var calls: Int = 0
        override fun processContext(requestTemplate: RequestTemplate, context: KudosContext) {
            calls++
            requestTemplate.header("X-Test-Processor", "fired-$calls")
        }
    }

    /**
     * 带 Spring 顺序值的请求上下文处理器测试实现。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class OrderedRecordingProcessor(
        private val name: String,
        private val order: Int,
        private val calls: MutableList<String>
    ) : IFeignRequestContextProcess, Ordered {
        override fun getOrder(): Int = order

        override fun processContext(requestTemplate: RequestTemplate, context: KudosContext) {
            calls += name
        }
    }
}
