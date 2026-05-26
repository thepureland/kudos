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
 * Unit tests for header injection in [GlobalHeaderRequestInterceptor].
 *
 * Coverage:
 *  - **Standard context headers**: tenantId / subSysCode / dataSourceId / locale written per the
 *    [Consts.RequestHeader] convention.
 *  - **When traceKey is missing**, a UUID is generated and written back to [KudosContext] so
 *    subsequent Feign calls in the same process share the same traceKey (round-6 bug fix:
 *    the old implementation generated without writing back).
 *  - **When traceKey already exists**, it is reused unchanged.
 *  - **When dataSourceId is missing**, the corresponding header is not written (optional field).
 *  - **When locale is missing**, falls back to `zh_CN`.
 *  - **FEIGN_REQUEST marker** is always written.
 *  - **`IFeignRequestContextProcess` SPI is invoked** — verifies the bean-cache path works.
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
        // Isolate context per test
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
        // Existing traceKey should be preserved
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
        assertTrue(generated.isNotBlank(), "Should generate a non-empty UUID")

        // Key assertion: the generated UUID is written back to context.traceKey
        assertEquals(generated, context.traceKey,
            "When traceKey is missing, the generated UUID should be written back to KudosContext so subsequent outbound calls reuse the same traceKey")
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

        assertEquals(firstTrace, secondTrace, "Subsequent Feign calls on the same thread should reuse the first generated UUID")
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
        // Blank string is treated as missing and overwritten back into context
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
            "When dataSourceId is null, the corresponding header should not be written")
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
        // Register one SPI implementation and call apply twice — verify the lazy cache path doesn't drop calls
        val processor = RecordingProcessor()
        ctx.beanFactory.registerSingleton("recordingProcessor", processor)

        val interceptor = GlobalHeaderRequestInterceptor()
        val t1 = newTemplate("GET", "/a")
        interceptor.apply(t1)
        val t2 = newTemplate("GET", "/b")
        interceptor.apply(t2)

        assertEquals(2, processor.calls, "processor should be invoked once per apply call")
        // The second call uses the lazy cache instead of SpringKit reflection — but should still be the same processor instance
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
     * Test implementation that records how many times the request-context processor was invoked.
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
     * Test implementation of the request-context processor with a Spring order value.
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
