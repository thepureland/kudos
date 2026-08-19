package io.kudos.ability.distributed.client.http.interceptor

import io.kudos.ability.distributed.client.http.init.properties.HttpClientProperties
import io.kudos.ability.distributed.client.http.support.IHttpRequestContextProcess
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import org.springframework.context.support.StaticApplicationContext
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for header injection in [KudosContextRequestInterceptor].
 *
 * Deliberately mirrors `GlobalHeaderRequestInterceptorTest` in the client-feign module case for case,
 * so that any behavioral drift between the two transports shows up as a failing assertion rather than
 * as a production surprise during the migration.
 *
 * The one intentional difference is the signed URL: this interceptor signs the **path**, whereas the
 * Feign test pins an absolute URL. See [KudosContextRequestInterceptor.signedUrl] — the provider's
 * `InternalRpcContextSignatureVerifier` rebuilds candidates from `HttpServletRequest.requestURI`, which is
 * always path-only, so the path form is the one that actually verifies.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosContextRequestInterceptorTest {

    private lateinit var ctx: StaticApplicationContext
    private lateinit var interceptor: KudosContextRequestInterceptor

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        interceptor = KudosContextRequestInterceptor()
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

        val request = newRequest(HttpMethod.GET, "http://svc/users/1")
        interceptor.intercept(request, ByteArray(0), NoopExecution)

        assertEquals("tenant-X", request.headers.getFirst(Consts.RequestHeader.TENANT_ID))
        assertEquals("sys-A", request.headers.getFirst(Consts.RequestHeader.SUB_SYS_CODE))
        assertEquals("trace-existing", request.headers.getFirst(Consts.RequestHeader.TRACE_KEY))
        assertEquals("ds-1", request.headers.getFirst(Consts.RequestHeader.DATASOURCE_ID))
        assertEquals("en_US", request.headers.getFirst(Consts.RequestHeader.LOCAL))
        assertEquals("true", request.headers.getFirst(Consts.RequestHeader.RPC_REQUEST))
        assertEquals("trace-existing", context.traceKey)
    }

    @Test
    fun executionIsAlwaysInvoked() {
        val execution = RecordingExecution()
        interceptor.intercept(newRequest(HttpMethod.GET, "http://svc/x"), ByteArray(0), execution)
        assertEquals(1, execution.calls)
    }

    @Test
    fun missingTraceKey_generatesUuid_andWritesBackToContext() {
        val context = KudosContextHolder.get()
        context.tenantId = "t"
        assertNull(context.traceKey)

        val request = newRequest(HttpMethod.GET, "http://svc/x")
        interceptor.intercept(request, ByteArray(0), NoopExecution)

        val generated = request.headers.getFirst(Consts.RequestHeader.TRACE_KEY)
        assertNotNull(generated)
        assertTrue(generated.isNotBlank())
        assertEquals(
            generated, context.traceKey,
            "A generated traceKey must be written back so later outbound calls stay on one trace"
        )
    }

    @Test
    fun secondCall_reusesGeneratedTraceKey_fromContext() {
        KudosContextHolder.get().tenantId = "t"

        val first = newRequest(HttpMethod.GET, "http://svc/a")
        interceptor.intercept(first, ByteArray(0), NoopExecution)
        val second = newRequest(HttpMethod.GET, "http://svc/b")
        interceptor.intercept(second, ByteArray(0), NoopExecution)

        assertEquals(
            first.headers.getFirst(Consts.RequestHeader.TRACE_KEY),
            second.headers.getFirst(Consts.RequestHeader.TRACE_KEY)
        )
    }

    @Test
    fun blankTraceKey_treatedAsAbsent_generatesAndWritesBack() {
        val context = KudosContextHolder.get()
        context.traceKey = "   "

        val request = newRequest(HttpMethod.GET, "http://svc/x")
        interceptor.intercept(request, ByteArray(0), NoopExecution)

        val generated = request.headers.getFirst(Consts.RequestHeader.TRACE_KEY)
        assertNotNull(generated)
        assertTrue(generated.isNotBlank())
        assertEquals(generated, context.traceKey)
    }

    @Test
    fun missingDataSourceId_skipsHeader() {
        KudosContextHolder.get().apply {
            tenantId = "t"
            dataSourceId = null
        }
        val request = newRequest(HttpMethod.GET, "http://svc/x")
        interceptor.intercept(request, ByteArray(0), NoopExecution)
        assertNull(request.headers.getFirst(Consts.RequestHeader.DATASOURCE_ID))
    }

    @Test
    fun missingSubSysCode_skipsHeader() {
        // `HttpHeaders.set(name, null)` would still emit the header with an empty value, which the
        // provider reads as "" rather than absent — an observable difference from Feign.
        KudosContextHolder.get().apply {
            tenantId = "t"
            subSystemCode = null
        }
        val request = newRequest(HttpMethod.GET, "http://svc/x")
        interceptor.intercept(request, ByteArray(0), NoopExecution)
        assertNull(request.headers.getFirst(Consts.RequestHeader.SUB_SYS_CODE))
        assertNull(request.headers[Consts.RequestHeader.SUB_SYS_CODE])
    }

    @Test
    fun missingLocale_defaultsToZhCN() {
        val request = newRequest(HttpMethod.GET, "http://svc/x")
        interceptor.intercept(request, ByteArray(0), NoopExecution)
        assertEquals("zh_CN", request.headers.getFirst(Consts.RequestHeader.LOCAL))
    }

    @Test
    fun contextSignatureSecretBlank_doesNotWriteSignatureHeaders() {
        val request = newRequest(HttpMethod.GET, "http://svc/x")

        KudosContextRequestInterceptor(HttpClientProperties())
            .intercept(request, ByteArray(0), NoopExecution)

        assertNull(request.headers.getFirst(HttpContextSignature.TIMESTAMP_HEADER))
        assertNull(request.headers.getFirst(HttpContextSignature.NONCE_HEADER))
        assertNull(request.headers.getFirst(HttpContextSignature.SIGNATURE_HEADER))
    }

    @Test
    fun contextSignatureSecretWhitespaceOnly_doesNotWriteSignatureHeaders() {
        val properties = HttpClientProperties().apply { contextSignatureSecret = "   " }
        val request = newRequest(HttpMethod.GET, "http://svc/x")

        KudosContextRequestInterceptor(properties).intercept(request, ByteArray(0), NoopExecution)

        assertNull(request.headers.getFirst(HttpContextSignature.TIMESTAMP_HEADER))
        assertNull(request.headers.getFirst(HttpContextSignature.NONCE_HEADER))
        assertNull(request.headers.getFirst(HttpContextSignature.SIGNATURE_HEADER))
    }

    @Test
    fun contextSignatureSecretPresent_signsPathNotAbsoluteUrl() {
        KudosContextHolder.get().apply {
            tenantId = "tenant-X"
            subSystemCode = "sys-A"
            traceKey = "trace-existing"
            dataSourceId = "ds-1"
            clientInfo = ClientInfo(ClientInfo.Builder()).apply { locale = Locale.US }
        }
        val request = newRequest(HttpMethod.POST, "http://10.0.0.1:8080/users")

        signingInterceptor().intercept(request, ByteArray(0), NoopExecution)

        assertEquals("123456789", request.headers.getFirst(HttpContextSignature.TIMESTAMP_HEADER))
        assertEquals("nonce-1", request.headers.getFirst(HttpContextSignature.NONCE_HEADER))
        // "/users", NOT "http://10.0.0.1:8080/users" — the host is whatever the LoadBalancer picked,
        // and the provider can only reconstruct the path.
        val expected = expectedSignature(
            "POST", "/users", "tenant-X", "sys-A", "trace-existing", "ds-1", "en_US"
        )
        assertEquals(expected, request.headers.getFirst(HttpContextSignature.SIGNATURE_HEADER))
    }

    @Test
    fun contextSignatureSecretPresent_signedPathIncludesQueryString() {
        KudosContextHolder.get().apply {
            tenantId = "tenant-X"
            subSystemCode = "sys-A"
            traceKey = "trace-existing"
        }
        val request = newRequest(HttpMethod.GET, "http://svc/users?page=2&size=10")

        signingInterceptor().intercept(request, ByteArray(0), NoopExecution)

        val expected = expectedSignature(
            "GET", "/users?page=2&size=10", "tenant-X", "sys-A", "trace-existing", "", "zh_CN"
        )
        assertEquals(expected, request.headers.getFirst(HttpContextSignature.SIGNATURE_HEADER))
    }

    @Test
    fun contextSignatureSecretPresent_missingOptionalHeaders_useEmptyStringInPayload() {
        KudosContextHolder.get().apply {
            tenantId = "tenant-X"
            subSystemCode = "sys-A"
            traceKey = "trace-existing"
            dataSourceId = null
        }
        val request = newRequest(HttpMethod.POST, "http://svc/users")

        signingInterceptor().intercept(request, ByteArray(0), NoopExecution)

        assertNull(request.headers.getFirst(Consts.RequestHeader.DATASOURCE_ID))
        val expected = expectedSignature(
            "POST", "/users", "tenant-X", "sys-A", "trace-existing", "", "zh_CN"
        )
        assertEquals(expected, request.headers.getFirst(HttpContextSignature.SIGNATURE_HEADER))
    }

    @Test
    fun extensionProcessorIsInvoked_onEveryCall() {
        val processor = RecordingProcessor()
        ctx.beanFactory.registerSingleton("recordingProcessor", processor)

        val interceptor = KudosContextRequestInterceptor()
        interceptor.intercept(newRequest(HttpMethod.GET, "http://svc/a"), ByteArray(0), NoopExecution)
        interceptor.intercept(newRequest(HttpMethod.GET, "http://svc/b"), ByteArray(0), NoopExecution)

        assertEquals(2, processor.calls, "processor should be invoked once per intercept call")
    }

    @Test
    fun extensionProcessorsAreAppliedInSpringOrder() {
        val calls = mutableListOf<String>()
        ctx.beanFactory.registerSingleton("laterProcessor", OrderedRecordingProcessor("later", 20, calls))
        ctx.beanFactory.registerSingleton("earlierProcessor", OrderedRecordingProcessor("earlier", 10, calls))

        KudosContextRequestInterceptor()
            .intercept(newRequest(HttpMethod.GET, "http://svc/ordered"), ByteArray(0), NoopExecution)

        assertEquals(listOf("earlier", "later"), calls)
    }

    @Test
    fun extensionProcessorHeadersAreCoveredByTheRequestItSees() {
        ctx.beanFactory.registerSingleton("xidProcessor", HeaderWritingProcessor("TX_XID", "xid-1"))

        val request = newRequest(HttpMethod.GET, "http://svc/x")
        KudosContextRequestInterceptor().intercept(request, ByteArray(0), NoopExecution)

        assertEquals("xid-1", request.headers.getFirst("TX_XID"))
    }

    private fun signingInterceptor() = KudosContextRequestInterceptor(
        properties = HttpClientProperties().apply { contextSignatureSecret = SECRET },
        clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneOffset.UTC),
        nonceSupplier = { "nonce-1" }
    )

    /** Recomputes the documented payload independently of the production code. */
    private fun expectedSignature(
        method: String,
        url: String,
        tenantId: String,
        subSysCode: String,
        traceKey: String,
        dataSourceId: String,
        locale: String
    ): String {
        val payload = listOf(
            method, url, tenantId, subSysCode, traceKey, dataSourceId, locale, "123456789", "nonce-1"
        ).joinToString("\n")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    private fun newRequest(method: HttpMethod, url: String) = MockClientHttpRequest(method, URI.create(url))

    private object NoopExecution : ClientHttpRequestExecution {
        override fun execute(request: HttpRequest, body: ByteArray): ClientHttpResponse =
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
    }

    private class RecordingExecution : ClientHttpRequestExecution {
        var calls: Int = 0
        override fun execute(request: HttpRequest, body: ByteArray): ClientHttpResponse {
            calls++
            return MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }
    }

    private class RecordingProcessor : IHttpRequestContextProcess {
        var calls: Int = 0
        override fun processContext(request: HttpRequest, context: KudosContext) {
            calls++
        }
    }

    private class HeaderWritingProcessor(
        private val name: String,
        private val value: String
    ) : IHttpRequestContextProcess {
        override fun processContext(request: HttpRequest, context: KudosContext) {
            request.headers.add(name, value)
        }
    }

    private class OrderedRecordingProcessor(
        private val id: String,
        private val order: Int,
        private val calls: MutableList<String>
    ) : IHttpRequestContextProcess, Ordered {
        override fun processContext(request: HttpRequest, context: KudosContext) {
            calls += id
        }

        override fun getOrder(): Int = order
    }

    private companion object {
        const val SECRET = "secret"
    }
}
