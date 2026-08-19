package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.context.support.Consts
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.mock.web.MockHttpServletRequest
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Direct unit tests for [InternalRpcContextSignatureVerifier].
 *
 * Coverage:
 *  - constructor `require` guards: blank secret / non-positive timestamp window / non-positive nonce cache size
 *  - missing or blank signature headers (timestamp / nonce / signature each) -> MISSING_SIGNATURE_HEADERS
 *  - non-numeric timestamp -> INVALID_TIMESTAMP
 *  - timestamp outside the +/- window (both past and future, plus exact boundary) -> TIMESTAMP_OUT_OF_WINDOW
 *  - wrong signature -> SIGNATURE_MISMATCH
 *  - valid signature with all context headers (incl. DATASOURCE_ID, Unicode tenant) -> OK
 *  - URL candidates: query string appended, servlet context path stripped, full request URI signed,
 *    context path that is not a prefix of the request URI
 *  - nonce replay -> NONCE_REPLAYED
 *  - nonce cache eviction when the size cap is exceeded (oldest evicted, accepted again)
 *  - expired nonces purged once the clock passes their expiry (no longer treated as replay)
 *  - header-absent (null) nonce / signature while the earlier headers are present
 *  - empty (non-null) query string is not appended to the signed URL candidates
 *  - null method / requestURI / contextPath are treated as empty strings
 *  - all context headers absent -> payload built from empty strings, still verifiable
 *  - concurrent verification of the same nonce accepts exactly one request (thread safety)
 *
 * @author K
 * @since 1.0.0
 */
internal class InternalRpcContextSignatureVerifierTest {

    @Test
    fun constructor_blankSecret_throws() {
        val e = assertFailsWith<IllegalArgumentException> {
            InternalRpcContextSignatureVerifier(secret = "   ")
        }
        assertTrue(e.message!!.contains("secret must not be blank"))
    }

    @Test
    fun constructor_nonPositiveTimestampWindow_throws() {
        val zero = assertFailsWith<IllegalArgumentException> {
            InternalRpcContextSignatureVerifier(secret = SECRET, timestampWindowMillis = 0)
        }
        assertTrue(zero.message!!.contains("timestampWindowMillis must be positive"))
        assertFailsWith<IllegalArgumentException> {
            InternalRpcContextSignatureVerifier(secret = SECRET, timestampWindowMillis = -1)
        }
    }

    @Test
    fun constructor_nonPositiveNonceCacheMaxSize_throws() {
        val e = assertFailsWith<IllegalArgumentException> {
            InternalRpcContextSignatureVerifier(secret = SECRET, nonceCacheMaxSize = 0)
        }
        assertTrue(e.message!!.contains("nonceCacheMaxSize must be positive"))
    }

    @Test
    fun verify_missingOrBlankSignatureHeaders_returnsMissing() {
        val verifier = fixedVerifier()

        // No signature headers at all
        val bare = MockHttpServletRequest("POST", URI)
        assertEquals(InternalRpcContextSignatureVerifier.Result.MISSING_SIGNATURE_HEADERS, verifier.verify(bare))

        // Blank timestamp
        val blankTimestamp = signedRequest(nonce = "n1").apply {
            removeHeader(InternalRpcContextSignatureVerifier.TIMESTAMP_HEADER)
            addHeader(InternalRpcContextSignatureVerifier.TIMESTAMP_HEADER, " ")
        }
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.MISSING_SIGNATURE_HEADERS,
            verifier.verify(blankTimestamp)
        )

        // Blank nonce
        val blankNonce = signedRequest(nonce = "n2").apply {
            removeHeader(InternalRpcContextSignatureVerifier.NONCE_HEADER)
            addHeader(InternalRpcContextSignatureVerifier.NONCE_HEADER, "")
        }
        assertEquals(InternalRpcContextSignatureVerifier.Result.MISSING_SIGNATURE_HEADERS, verifier.verify(blankNonce))

        // Blank signature
        val blankSignature = signedRequest(nonce = "n3").apply {
            removeHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER)
            addHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER, "")
        }
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.MISSING_SIGNATURE_HEADERS,
            verifier.verify(blankSignature)
        )
    }

    @Test
    fun verify_nonceHeaderAbsent_whileTimestampPresent_returnsMissing() {
        val verifier = fixedVerifier()
        val request = signedRequest(nonce = "n-absent").apply {
            removeHeader(InternalRpcContextSignatureVerifier.NONCE_HEADER)
        }
        assertEquals(InternalRpcContextSignatureVerifier.Result.MISSING_SIGNATURE_HEADERS, verifier.verify(request))
    }

    @Test
    fun verify_signatureHeaderAbsent_whileTimestampAndNoncePresent_returnsMissing() {
        val verifier = fixedVerifier()
        val request = signedRequest(nonce = "n-absent-sig").apply {
            removeHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER)
        }
        assertEquals(InternalRpcContextSignatureVerifier.Result.MISSING_SIGNATURE_HEADERS, verifier.verify(request))
    }

    @Test
    fun verify_nonNumericTimestamp_returnsInvalidTimestamp() {
        val verifier = fixedVerifier()
        val request = signedRequest(timestamp = "not-a-number", nonce = "n")
        assertEquals(InternalRpcContextSignatureVerifier.Result.INVALID_TIMESTAMP, verifier.verify(request))
    }

    @Test
    fun verify_timestampOutOfWindow_pastAndFuture_returnsOutOfWindow() {
        val verifier = fixedVerifier()
        val window = InternalRpcContextSignatureVerifier.DEFAULT_TIMESTAMP_WINDOW_MILLIS

        val tooOld = signedRequest(timestamp = (NOW - window - 1).toString(), nonce = "n-old")
        assertEquals(InternalRpcContextSignatureVerifier.Result.TIMESTAMP_OUT_OF_WINDOW, verifier.verify(tooOld))

        val tooNew = signedRequest(timestamp = (NOW + window + 1).toString(), nonce = "n-new")
        assertEquals(InternalRpcContextSignatureVerifier.Result.TIMESTAMP_OUT_OF_WINDOW, verifier.verify(tooNew))
    }

    @Test
    fun verify_timestampExactlyOnWindowBoundary_accepted() {
        val verifier = fixedVerifier()
        val window = InternalRpcContextSignatureVerifier.DEFAULT_TIMESTAMP_WINDOW_MILLIS
        val request = signedRequest(timestamp = (NOW - window).toString(), nonce = "n-boundary")
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_wrongSignature_returnsSignatureMismatch() {
        val verifier = fixedVerifier()
        val request = signedRequest(nonce = "n-bad").apply {
            removeHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER)
            addHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER, "definitely-not-the-signature")
        }
        assertEquals(InternalRpcContextSignatureVerifier.Result.SIGNATURE_MISMATCH, verifier.verify(request))
    }

    @Test
    fun verify_signatureComputedWithDifferentSecret_returnsSignatureMismatch() {
        val verifier = fixedVerifier()
        val request = signedRequest(nonce = "n-secret", secret = "another-secret")
        assertEquals(InternalRpcContextSignatureVerifier.Result.SIGNATURE_MISMATCH, verifier.verify(request))
    }

    @Test
    fun verify_validSignature_withAllContextHeaders_returnsOk() {
        val verifier = fixedVerifier()
        val request = signedRequest(
            nonce = "n-full",
            tenantId = "租戶-é中文", // Unicode tenant id
            subSysCode = "sys-a",
            traceKey = "trace-a",
            dataSourceId = "ds-a",
            local = "zh_CN"
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_queryStringIsPartOfSignedUrl_returnsOk() {
        val verifier = fixedVerifier()
        val request = signedRequest(
            nonce = "n-query",
            queryString = "a=1&b=2",
            signedUrl = "$URI?a=1&b=2"
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_servletContextPathStripped_matchesClientSignedRelativePath() {
        val verifier = fixedVerifier()
        // Client signed "/api/users" but the provider runs under context path "/app"
        val request = signedRequest(
            nonce = "n-ctx",
            requestUri = "/app$URI",
            contextPath = "/app",
            signedUrl = URI
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_servletContextPathStripped_withQueryString_returnsOk() {
        val verifier = fixedVerifier()
        val request = signedRequest(
            nonce = "n-ctx-query",
            requestUri = "/app$URI",
            contextPath = "/app",
            queryString = "q=v",
            signedUrl = "$URI?q=v"
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_fullRequestUriSigned_alsoAccepted_whenContextPathPresent() {
        val verifier = fixedVerifier()
        // Some clients may sign the full URI including the context path — first candidate matches
        val request = signedRequest(
            nonce = "n-full-uri",
            requestUri = "/app$URI",
            contextPath = "/app",
            signedUrl = "/app$URI"
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_contextPathNotPrefixOfRequestUri_onlyRawUriCandidate() {
        val verifier = fixedVerifier()
        val request = signedRequest(
            nonce = "n-no-prefix",
            requestUri = URI,
            contextPath = "/other",
            signedUrl = URI
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_emptyQueryString_notAppendedToSignedUrl() {
        val verifier = fixedVerifier()
        // queryString is "" (non-null but empty) -> the candidate URL must stay without "?"
        val request = signedRequest(
            nonce = "n-empty-query",
            queryString = "",
            signedUrl = URI
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_nullMethodAndRequestUri_treatedAsEmptyStrings() {
        val verifier = fixedVerifier()
        val nonce = "n-null-method"
        // Client payload built over empty method and empty url
        val payload = listOf("", "", "", "", "", "", "", NOW.toString(), nonce).joinToString("\n")
        val request = MockHttpServletRequest(null, null).apply {
            addHeader(InternalRpcContextSignatureVerifier.TIMESTAMP_HEADER, NOW.toString())
            addHeader(InternalRpcContextSignatureVerifier.NONCE_HEADER, nonce)
            addHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER, hmacSha256(payload, SECRET))
        }
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_nullContextPath_treatedAsEmpty() {
        val verifier = fixedVerifier()
        val request = NullContextPathRequest(signedRequest(nonce = "n-null-ctx"))
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_allContextHeadersAbsent_payloadUsesEmptyStrings_returnsOk() {
        val verifier = fixedVerifier()
        val request = signedRequest(nonce = "n-no-context", tenantId = null)
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(request))
    }

    @Test
    fun verify_concurrentSameNonce_exactlyOneAccepted() {
        val verifier = fixedVerifier()
        val threadCount = 8
        val barrier = CyclicBarrier(threadCount)
        val results = ConcurrentLinkedQueue<InternalRpcContextSignatureVerifier.Result>()
        val threads = (1..threadCount).map {
            Thread {
                barrier.await()
                results += verifier.verify(signedRequest(nonce = "n-concurrent"))
            }
        }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)
        assertEquals(threadCount, results.size)
        assertEquals(
            1,
            results.count { it == InternalRpcContextSignatureVerifier.Result.OK },
            "exactly one concurrent request with the same nonce may be accepted"
        )
        assertEquals(
            threadCount - 1,
            results.count { it == InternalRpcContextSignatureVerifier.Result.NONCE_REPLAYED }
        )
    }

    @Test
    fun verify_replayedNonce_returnsNonceReplayed() {
        val verifier = fixedVerifier()
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(signedRequest(nonce = "n-replay")))
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.NONCE_REPLAYED,
            verifier.verify(signedRequest(nonce = "n-replay"))
        )
    }

    @Test
    fun verify_nonceCacheEviction_oldestNonceAcceptedAgainAfterCapExceeded() {
        val verifier = InternalRpcContextSignatureVerifier(
            secret = SECRET,
            nonceCacheMaxSize = 2,
            clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
        )
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(signedRequest(nonce = "n-a")))
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(signedRequest(nonce = "n-b")))
        // Third unique nonce evicts the oldest ("n-a") because the cap is 2
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(signedRequest(nonce = "n-c")))
        // "n-a" was evicted, so its replay is (deliberately) accepted again; "n-c" is still cached
        assertEquals(InternalRpcContextSignatureVerifier.Result.OK, verifier.verify(signedRequest(nonce = "n-a")))
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.NONCE_REPLAYED,
            verifier.verify(signedRequest(nonce = "n-c"))
        )
    }

    @Test
    fun verify_expiredNoncesPurged_notTreatedAsReplayAfterWindowPassed() {
        val clock = MutableClock(Instant.ofEpochMilli(NOW))
        val window = 1_000L
        val verifier = InternalRpcContextSignatureVerifier(
            secret = SECRET,
            timestampWindowMillis = window,
            clock = clock
        )
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.OK,
            verifier.verify(signedRequest(timestamp = NOW.toString(), nonce = "n-exp"))
        )
        // Advance past the nonce expiry (timestamp + window); the old entry must be purged
        val later = NOW + window + window + 1
        clock.instant = Instant.ofEpochMilli(later)
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.OK,
            verifier.verify(signedRequest(timestamp = later.toString(), nonce = "n-other"))
        )
        // The expired "n-exp" is no longer in the cache -> accepted as fresh again
        assertEquals(
            InternalRpcContextSignatureVerifier.Result.OK,
            verifier.verify(signedRequest(timestamp = later.toString(), nonce = "n-exp"))
        )
    }

    private fun fixedVerifier() = InternalRpcContextSignatureVerifier(
        secret = SECRET,
        clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    )

    /**
     * Build a request whose signature headers are computed the way the client-side interceptor
     * computes them. [signedUrl] is the URL string covered by the signature (defaults to
     * [requestUri], plus the query string when present); the actual request URI / context path /
     * query string are configured independently so URL-candidate matching can be exercised.
     */
    private fun signedRequest(
        method: String = "POST",
        requestUri: String = URI,
        contextPath: String = "",
        queryString: String? = null,
        signedUrl: String = if (queryString.isNullOrEmpty()) requestUri else "$requestUri?$queryString",
        tenantId: String? = "tenant-a",
        subSysCode: String? = null,
        traceKey: String? = null,
        dataSourceId: String? = null,
        local: String? = null,
        timestamp: String = NOW.toString(),
        nonce: String,
        secret: String = SECRET
    ): MockHttpServletRequest {
        val payload = listOf(
            method,
            signedUrl,
            tenantId.orEmpty(),
            subSysCode.orEmpty(),
            traceKey.orEmpty(),
            dataSourceId.orEmpty(),
            local.orEmpty(),
            timestamp,
            nonce
        ).joinToString("\n")
        return MockHttpServletRequest(method, requestUri).apply {
            this.contextPath = contextPath
            if (queryString != null) this.queryString = queryString
            tenantId?.let { addHeader(Consts.RequestHeader.TENANT_ID, it) }
            subSysCode?.let { addHeader(Consts.RequestHeader.SUB_SYS_CODE, it) }
            traceKey?.let { addHeader(Consts.RequestHeader.TRACE_KEY, it) }
            dataSourceId?.let { addHeader(Consts.RequestHeader.DATASOURCE_ID, it) }
            local?.let { addHeader(Consts.RequestHeader.LOCAL, it) }
            addHeader(InternalRpcContextSignatureVerifier.TIMESTAMP_HEADER, timestamp)
            addHeader(InternalRpcContextSignatureVerifier.NONCE_HEADER, nonce)
            addHeader(InternalRpcContextSignatureVerifier.SIGNATURE_HEADER, hmacSha256(payload, secret))
        }
    }

    private fun hmacSha256(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    /**
     * Request wrapper whose servlet context path is null (allowed by the servlet API before the
     * request is fully initialized) — exercises the `contextPath.orEmpty()` branch.
     *
     * @author K
     * @since 1.0.0
     */
    private class NullContextPathRequest(delegate: HttpServletRequest) : HttpServletRequestWrapper(delegate) {
        override fun getContextPath(): String? = null
    }

    /**
     * Mutable test clock so a single verifier instance can observe time moving forward.
     *
     * @author K
     * @since 1.0.0
     */
    private class MutableClock(var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = instant
    }

    companion object {
        private const val SECRET = "verifier-secret"
        private const val URI = "/api/users"
        private const val NOW = 1_700_000_000_000L
    }
}
