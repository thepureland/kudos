package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import org.springframework.context.support.StaticApplicationContext
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for HMAC signature enforcement in [FeignContextWebFilter] +
 * [FeignContextSignatureVerifier].
 *
 * The signing helper mirrors the client-side `GlobalHeaderRequestInterceptor.signaturePayload`
 * contract (module `kudos-ability-distributed-client-feign`): HMAC-SHA256 (Base64) over
 * method / url / TENANT_ID / SUB_SYS_CODE / TRACE_KEY / DATASOURCE_ID / LOCAL / timestamp / nonce
 * joined with line feeds.
 *
 * Coverage:
 *  - valid signature passes and the context headers are written back
 *  - tampered context header (signature mismatch) is rejected with 401, context untouched
 *  - expired timestamp is rejected with 401
 *  - nonce replay is rejected with 401 (first request passes)
 *  - missing signature headers while a secret is configured is rejected with 401
 *  - no secret configured keeps the legacy behavior (unsigned requests pass)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class FeignContextWebFilterSignatureTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        KudosContextHolder.clear()
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
        ctx.close()
    }

    @Test
    fun validSignature_passes_andRestoresContext() {
        val filter = filterWithVerifier()
        val request = signedRequest(nonce = "nonce-valid")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNotNull(chain.request, "filter chain should continue for an authentic request")
        assertEquals(200, response.status)
        val context = KudosContextHolder.get()
        assertEquals("tenant-a", context.tenantId)
        assertEquals("sys-a", context.subSystemCode)
        assertEquals("trace-a", context.traceKey)
    }

    @Test
    fun tamperedTenantHeader_rejectedWith401_andContextUntouched() {
        val filter = filterWithVerifier()
        // Signature computed over tenant-a, but the request actually carries tenant-victim —
        // classic forged-tenant attack.
        val request = signedRequest(nonce = "nonce-tampered", sentTenantId = "tenant-victim")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(chain.request, "filter chain must not continue for a tampered request")
        assertEquals(401, response.status)
        assertNull(KudosContextHolder.get().tenantId, "context must not be written for a rejected request")
    }

    @Test
    fun expiredTimestamp_rejectedWith401() {
        val filter = filterWithVerifier()
        val expired = NOW_MILLIS - FeignContextSignatureVerifier.DEFAULT_TIMESTAMP_WINDOW_MILLIS - 1
        val request = signedRequest(timestamp = expired.toString(), nonce = "nonce-expired")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(chain.request)
        assertEquals(401, response.status)
        assertNull(KudosContextHolder.get().tenantId)
    }

    @Test
    fun replayedNonce_rejectedWith401_firstRequestPasses() {
        val filter = filterWithVerifier()

        val first = MockHttpServletResponse()
        filter.doFilter(signedRequest(nonce = "nonce-replayed"), first, MockFilterChain())
        assertEquals(200, first.status, "first request with a fresh nonce should pass")

        KudosContextHolder.clear()
        val replayChain = MockFilterChain()
        val replay = MockHttpServletResponse()
        filter.doFilter(signedRequest(nonce = "nonce-replayed"), replay, replayChain)

        assertNull(replayChain.request)
        assertEquals(401, replay.status)
        assertNull(KudosContextHolder.get().tenantId)
    }

    @Test
    fun missingSignatureHeaders_whenSecretConfigured_rejectedWith401() {
        val filter = filterWithVerifier()
        val request = MockHttpServletRequest("POST", REQUEST_URI).apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-a")
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(chain.request)
        assertEquals(401, response.status)
        assertNull(KudosContextHolder.get().tenantId)
    }

    @Test
    fun noSecretConfigured_unsignedRequestStillPasses() {
        val filter = FeignContextWebFilter()
        val request = MockHttpServletRequest("POST", REQUEST_URI).apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-legacy")
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNotNull(chain.request, "legacy mode (no secret) must keep accepting unsigned requests")
        assertEquals(200, response.status)
        assertEquals("tenant-legacy", KudosContextHolder.get().tenantId)
    }

    private fun filterWithVerifier(): FeignContextWebFilter {
        val verifier = FeignContextSignatureVerifier(
            secret = SECRET,
            clock = Clock.fixed(Instant.ofEpochMilli(NOW_MILLIS), ZoneOffset.UTC)
        )
        return FeignContextWebFilter(signatureVerifier = verifier)
    }

    /**
     * Build a Feign-marked request whose signature headers are computed exactly the way the
     * client-side interceptor computes them (always signed over tenant-a). [sentTenantId] lets a
     * test send a TENANT_ID header that differs from the signed value to simulate tampering.
     */
    private fun signedRequest(
        timestamp: String = NOW_MILLIS.toString(),
        nonce: String,
        sentTenantId: String = SIGNED_TENANT_ID
    ): MockHttpServletRequest {
        val payload = listOf(
            "POST",
            REQUEST_URI,
            SIGNED_TENANT_ID,
            "sys-a",
            "trace-a",
            "",
            "en_US",
            timestamp,
            nonce
        ).joinToString("\n")
        return MockHttpServletRequest("POST", REQUEST_URI).apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, sentTenantId)
            addHeader(Consts.RequestHeader.SUB_SYS_CODE, "sys-a")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-a")
            addHeader(Consts.RequestHeader.LOCAL, "en_US")
            addHeader(FeignContextSignatureVerifier.TIMESTAMP_HEADER, timestamp)
            addHeader(FeignContextSignatureVerifier.NONCE_HEADER, nonce)
            addHeader(FeignContextSignatureVerifier.SIGNATURE_HEADER, hmacSha256(payload))
        }
    }

    private fun hmacSha256(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    companion object {
        private const val SECRET = "test-secret"
        private const val REQUEST_URI = "/api/users"
        private const val SIGNED_TENANT_ID = "tenant-a"
        private const val NOW_MILLIS = 1_700_000_000_000L
    }
}
