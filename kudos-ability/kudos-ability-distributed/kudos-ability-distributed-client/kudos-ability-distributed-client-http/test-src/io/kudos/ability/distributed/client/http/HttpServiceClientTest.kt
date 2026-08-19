package io.kudos.ability.distributed.client.http

import io.kudos.ability.distributed.client.http.client.ICauseAwareClient
import io.kudos.ability.distributed.client.http.client.IHttpTestClient
import io.kudos.ability.distributed.client.http.client.ISecondaryTestClient
import io.kudos.ability.distributed.client.http.client.IUnreachableClient
import io.kudos.ability.distributed.client.http.client.UnreachableClientFallback
import io.kudos.ability.distributed.client.http.ms.MockMsApplication
import io.kudos.context.core.KudosContextHolder
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.NacosTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import io.kudos.context.support.Consts
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.service.registry.ImportHttpServices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for Spring interface clients — the interface-client counterpart of the
 * client-feign module's `OpenFeignTest`, and the proof that this module can replace it.
 *
 * What it covers beyond a plain "the call works":
 * 1. **Service discovery through `lb://`** — the client is bound to `lb://test-http-service`, so a
 *    successful call means `LoadBalancerRestClientHttpServiceGroupConfigurer` resolved the instance
 *    from Nacos, i.e. `@FeignClient(name = ...)` has a working replacement.
 * 2. **Context propagation over the wire** — `/echoHeaders` returns what the provider actually
 *    received, so this asserts the real HTTP headers, not a mocked `RequestTemplate`.
 * 3. **Fallback** — `IUnreachableClient` points at a dead port and is bound to a
 *    `@HttpServiceFallback`, which is the replacement for `@FeignClient(fallback = ...)`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@ActiveProfiles("client")
@Import(
    UnreachableClientConfiguration::class,
    SecondaryClientConfiguration::class,
    CauseAwareClientConfiguration::class,
)
@ImportHttpServices(group = "testservice", types = [IHttpTestClient::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class HttpServiceClientTest {

    @Autowired
    private lateinit var httpClient: IHttpTestClient

    @Autowired
    private lateinit var unreachableClient: IUnreachableClient

    @Autowired
    private lateinit var secondaryClient: ISecondaryTestClient

    @Autowired
    private lateinit var causeAwareClient: ICauseAwareClient

    private var msPort: Int = 0

    @BeforeAll
    fun setup() {
        NacosTestContainer.startIfNeeded(null)
        println("########## Starting the mock microservice...")
        val msContext = SpringApplication.run(MockMsApplication::class.java)
        msPort = (msContext as ServletWebServerApplicationContext).webServer!!.port
        println("########## Mock microservice started on port $msPort.")
    }

    @Test
    fun `GET and POST resolve through lb-- and round-trip`() {
        assertTrue(httpClient.get())

        val result = httpClient.post(PostParam(1, "name"))
        assertEquals(true, result.success)
    }

    @Test
    fun `remote exception propagates to the caller`() {
        assertFailsWith<RuntimeException> { httpClient.exception() }
    }

    @Test
    fun `kudos context headers reach the provider`() {
        val context = KudosContextHolder.get().apply {
            tenantId = "tenant-e2e"
            subSystemCode = "subsys-e2e"
        }

        val received = httpClient.echoHeaders()

        assertEquals("true", received["internalMarker"])
        // 用真实值断言。此前两个字段都是 null，"null" == "null" 让这条断言形同虚设，
        // 掩盖了"限时器线程池会不会切断 ThreadLocal 上下文"这个问题。
        assertEquals("tenant-e2e", received["tenantId"])
        assertEquals("subsys-e2e", received["subSysCode"])
        assertEquals("zh_CN", received["local"])
        // A trace key is always present on the wire, generated when the context has none.
        val traceKey = assertNotNull(received["traceKey"])
        assertTrue(traceKey.isNotBlank())
    }

    // NOTE: the write-back half of the trace-key rule — that the generated key is stored back into
    // KudosContext so later outbound calls of one logical request share it — is asserted in
    // KudosContextRequestInterceptorTest, not here. It cannot be asserted end-to-end in this class:
    // the mock provider is a second Spring application in the same JVM, and kudos context plumbing is
    // held in JVM-wide statics (SpringKit.applicationContext, the context holder), so the two apps
    // interfere. Asserting it here measured the harness, not the product.

    @Test
    fun `a slow call is cut off by the group read-timeout, not by the TimeLimiter`() {
        // The module defaults Resilience4j to `disable-thread-pool: true` so that the call runs on the
        // caller's thread and the KudosContext survives (see the test above). The cost is that the
        // TimeLimiter can no longer interrupt anything — verified: with the thread pool on, a 3s call
        // died at 1s with `TimeLimiter recorded a timeout exception`; with it off the same call ran to
        // completion until this group grew a `read-timeout`.
        //
        // So the group's read-timeout is the real bound, and this pins that it is doing the work.
        val error = assertFailsWith<Exception> { httpClient.slow() }
        val chain = generateSequence(error as Throwable) { it.cause }.toList()
        assertTrue(
            chain.any { it is ResourceAccessException },
            "expected a read timeout from the HTTP layer, got: ${chain.map { it.javaClass.simpleName }}"
        )
        assertTrue(
            chain.none { it.message?.contains("TimeLimiter") == true },
            "the TimeLimiter must not be what cuts the call off — it runs the call off-thread and loses the context"
        )
    }

    @Test
    fun `the client signs context headers and the provider verifies them`() {
        // Client side: the HMAC headers really went on the wire.
        val signature = httpClient.echoSignature()
        assertNotNull(signature["timestamp"], "timestamp header must be signed onto the request")
        assertNotNull(signature["nonce"], "nonce header must be signed onto the request")
        assertNotNull(signature["signature"], "signature header must be signed onto the request")

        // Provider side: an internal-marked request with NO signature must be rejected. Without this,
        // a green test would equally be explained by the verifier never having been registered — which
        // is exactly how the signing path stayed unverified end-to-end for so long.
        val status = RestClient.create()
            .get()
            .uri("http://localhost:$msPort/testHttpService/get")
            .header(Consts.RequestHeader.RPC_REQUEST, "true")
            .exchange { _, response -> response.statusCode }
        assertEquals(HttpStatusCode.valueOf(401), status, "provider must reject an unsigned internal request")
    }

    @Test
    fun `a second ImportHttpServices merges into the same group`() {
        // Declared by SecondaryClientConfiguration, not by this class — a successful call proves the
        // group's base-url applies to registrations contributed from anywhere.
        assertTrue(secondaryClient.get())
    }

    @Test
    fun `unreachable service falls back instead of throwing`() {
        assertEquals(UnreachableClientFallback.FALLBACK_VALUE, unreachableClient.read())
    }

    @Test
    fun `a fallback may declare a leading Throwable to learn why it fired`() {
        // The interface-client replacement for Feign's FallbackFactory<T>.create(cause): the resolver
        // prefers a `name(Throwable, ...args)` overload over the plain contract signature.
        val result = causeAwareClient.read("k")
        assertNotNull(result)
        assertTrue(result.startsWith("k:"), "fallback should have run, got: $result")
        assertTrue(
            result.removePrefix("k:").isNotBlank(),
            "the triggering exception should have been passed in, got: $result"
        )
    }

}
