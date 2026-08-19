package io.kudos.ability.web.springmvc.filter

import io.kudos.ability.web.springmvc.init.SpringMvcProperties
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.support.Consts
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [WebContextInitFilter].
 *
 * Covers:
 *  - session / cookie / header copy into [KudosContext]
 *  - traceKey taken from the request header vs. generated when missing or blank
 *  - ClientInfo assembly (ip / domain / url / params / browser / os / referer / locale)
 *  - no-session requests never create a session
 *  - context is cleared after the chain completes, including when the chain throws
 *
 * @author K
 * @since 1.0.0
 */
internal class WebContextInitFilterTest {

    private val filter = WebContextInitFilter()

    @BeforeEach
    fun setUp() = KudosContextHolder.clear()

    @AfterEach
    fun tearDown() = KudosContextHolder.clear()

    /** Filter chain that captures the context visible to downstream business code. */
    private class CapturingChain : MockFilterChain() {
        var captured: KudosContext? = null
        override fun doFilter(request: ServletRequest, response: ServletResponse) {
            captured = KudosContextHolder.get()
        }
    }

    @Test
    fun doFilter_copiesSessionCookieHeaderAndClientInfo() {
        val request = MockHttpServletRequest("GET", "/api/users").apply {
            serverName = "kudos.example"
            remoteAddr = "8.8.8.8"
            addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0.0.0")
            addHeader("referer", "https://kudos.example/home")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-abc")
            addParameter("q", "kudos")
            setCookies(Cookie("token", "cookie-value"))
            setPreferredLocales(listOf(Locale.TRADITIONAL_CHINESE))
            getSession(true)!!.setAttribute("loginName", "will")
        }
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        val context = assertNotNull(chain.captured)
        assertEquals("will", context.sessionAttributes?.get("loginName"))
        assertEquals("cookie-value", context.cookieAttributes?.get("token"))
        assertEquals("trace-abc", context.headerAttributes?.get(Consts.RequestHeader.TRACE_KEY))
        assertEquals("trace-abc", context.traceKey)

        val clientInfo = assertNotNull(context.clientInfo)
        assertEquals("8.8.8.8", clientInfo.ip)
        assertEquals("kudos.example", clientInfo.domain)
        assertEquals("/api/users", clientInfo.url)
        assertEquals("kudos", clientInfo.params?.get("q")?.single())
        assertEquals("Chrome", clientInfo.browser?.first)
        assertEquals("Windows", clientInfo.os?.first)
        assertEquals("https://kudos.example/home", clientInfo.requestReferer)
        assertEquals(Locale.TRADITIONAL_CHINESE, clientInfo.locale)

        // The context must be cleared after the request completes.
        assertNull(KudosContextHolder.getOrNull())
    }

    @Test
    fun doFilter_withoutSessionCookieAndTraceKey_generatesTraceKey() {
        val request = MockHttpServletRequest("GET", "/anonymous")
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        val context = assertNotNull(chain.captured)
        assertNull(context.sessionAttributes)
        assertNull(context.cookieAttributes)
        assertFalse(context.traceKey.isNullOrBlank(), "traceKey should be generated when absent")
        // getSession(false) must never create a session for anonymous requests.
        assertNull(request.getSession(false))
    }

    @Test
    fun doFilter_blankTraceKeyHeader_generatesTraceKey() {
        val request = MockHttpServletRequest("GET", "/blank").apply {
            addHeader(Consts.RequestHeader.TRACE_KEY, "  ")
        }
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        val context = assertNotNull(chain.captured)
        assertFalse(context.traceKey.isNullOrBlank())
        assertFalse(context.traceKey == "  ")
    }

    /**
     * A caller-supplied trace key is interpolated into log lines and echoed into a response header, so it must
     * not be taken on trust: CR/LF in that value forges log entries and splits response headers, and an
     * unbounded length is a cheap lever against any log or metrics backend that indexes by trace id.
     */
    @Test
    fun doFilter_rejectsUnsafeTraceKeyAndGeneratesOne() {
        listOf(
            "trace\r\nX-Injected: evil",
            "trace\nforged log line",
            "a".repeat(129),
            "trace with spaces"
        ).forEach { unsafe ->
            val request = MockHttpServletRequest("GET", "/unsafe").apply {
                addHeader(Consts.RequestHeader.TRACE_KEY, unsafe)
            }
            val chain = CapturingChain()

            filter.doFilter(request, MockHttpServletResponse(), chain)

            val traceKey = assertNotNull(chain.captured).traceKey
            assertFalse(traceKey == unsafe, "Unsafe trace key should have been rejected: $unsafe")
            assertFalse(traceKey.isNullOrBlank())
        }
    }

    /** A well-formed caller trace key is the whole point of the header, so it must survive. */
    @Test
    fun doFilter_acceptsWellFormedCallerTraceKey() {
        val request = MockHttpServletRequest("GET", "/ok").apply {
            addHeader(Consts.RequestHeader.TRACE_KEY, "abc-123_def.456")
        }
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals("abc-123_def.456", assertNotNull(chain.captured).traceKey)
    }

    /**
     * The trace id must reach the caller on the response itself, not only inside a wrapped `ApiResponse` body —
     * otherwise it is missing from exactly the responses that need it most: non-JSON payloads,
     * `@IgnoreApiResponseWrap` endpoints, and errors that short-circuit before any advice runs.
     */
    @Test
    fun doFilter_echoesTraceIdOnResponseHeader() {
        val request = MockHttpServletRequest("GET", "/traced").apply {
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-abc")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, CapturingChain())

        assertEquals("trace-abc", response.getHeader("X-Trace-Id"))
    }

    /**
     * **The header a caller reads off the response must be one it can send back.**
     *
     * This filter used to read only the internal `_UUID` header while echoing `X-Trace-Id`, so a client that
     * did the obvious thing — take the trace id from the response, put it on the next request under the same
     * name — was silently issued a fresh id instead, breaking the chain it was trying to continue.
     */
    @Test
    fun doFilter_publicTraceHeaderRoundTrips() {
        val request = MockHttpServletRequest("GET", "/round-trip").apply {
            addHeader("X-Trace-Id", "trace-from-client")
        }
        val response = MockHttpServletResponse()
        val chain = CapturingChain()

        filter.doFilter(request, response, chain)

        assertEquals("trace-from-client", assertNotNull(chain.captured).traceKey)
        assertEquals("trace-from-client", response.getHeader("X-Trace-Id"))
    }

    /**
     * The internal Feign header must keep working untouched: `GlobalHeaderRequestInterceptor` sets it on every
     * outgoing service-to-service call and `InternalRpcContextSignatureVerifier` folds it into the request signature,
     * so every existing internal call has to resolve exactly as it did before.
     */
    @Test
    fun doFilter_internalFeignTraceHeaderStillResolves() {
        val request = MockHttpServletRequest("GET", "/internal").apply {
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-from-feign")
        }
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals("trace-from-feign", assertNotNull(chain.captured).traceKey)
    }

    /** With both present, the public header wins — it is the name the response advertises. */
    @Test
    fun doFilter_publicTraceHeaderTakesPrecedence() {
        val request = MockHttpServletRequest("GET", "/both").apply {
            addHeader("X-Trace-Id", "trace-public")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-internal")
        }
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals("trace-public", assertNotNull(chain.captured).traceKey)
    }

    /** A rejected value in one header must not discard a good value in the next. */
    @Test
    fun doFilter_unsafePublicHeader_fallsBackToInternalHeader() {
        val request = MockHttpServletRequest("GET", "/mixed").apply {
            addHeader("X-Trace-Id", "trace\r\nX-Injected: evil")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-internal")
        }
        val chain = CapturingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals("trace-internal", assertNotNull(chain.captured).traceKey)
    }

    /**
     * Renaming the public header must move both directions together; that they agree is the property being
     * fixed, so it must not depend on two settings happening to hold the same value.
     */
    @Test
    fun doFilter_renamedPublicHeader_staysSymmetric() {
        val renamed = WebContextInitFilter(
            SpringMvcProperties.Context().apply { traceIdHeader = "X-Request-Id" }
        )
        val request = MockHttpServletRequest("GET", "/renamed").apply {
            addHeader("X-Request-Id", "trace-renamed")
        }
        val response = MockHttpServletResponse()
        val chain = CapturingChain()

        renamed.doFilter(request, response, chain)

        assertEquals("trace-renamed", assertNotNull(chain.captured).traceKey)
        assertEquals("trace-renamed", response.getHeader("X-Request-Id"))
    }

    /** Blanking the public header opts out of it entirely: nothing echoed, only the internal header read. */
    @Test
    fun doFilter_blankPublicHeader_disablesEchoAndPublicRead() {
        val internalOnly = WebContextInitFilter(
            SpringMvcProperties.Context().apply { traceIdHeader = "" }
        )
        val request = MockHttpServletRequest("GET", "/internal-only").apply {
            addHeader("X-Trace-Id", "trace-public")
            addHeader(Consts.RequestHeader.TRACE_KEY, "trace-internal")
        }
        val response = MockHttpServletResponse()
        val chain = CapturingChain()

        internalOnly.doFilter(request, response, chain)

        assertEquals("trace-internal", assertNotNull(chain.captured).traceKey)
        assertNull(response.getHeader("X-Trace-Id"))
    }

    /** Excluded paths skip the session/cookie/header copy entirely. */
    @Test
    fun doFilter_excludedPath_skipsContextAssembly() {
        val excluding = WebContextInitFilter(
            SpringMvcProperties.Context().apply { excludePathPatterns = listOf("/_monitor/**") }
        )
        val request = MockHttpServletRequest("GET", "/_monitor/health").apply {
            addHeader("User-Agent", "probe")
        }
        val chain = CapturingChain()

        excluding.doFilter(request, MockHttpServletResponse(), chain)

        // Nothing was assembled, so the chain sees the empty auto-created context rather than a populated one.
        assertNull(assertNotNull(chain.captured).clientInfo)
        // ...and it is still cleared afterwards: KudosContextHolder.get() creates on read, so skipping the
        // cleanup for excluded paths would leak a context onto the pooled thread.
        assertNull(KudosContextHolder.getOrNull())
    }

    /** A non-HTTP request must pass straight through instead of failing on an unchecked cast. */
    @Test
    fun doFilter_nonHttpRequest_passesThrough() {
        val chain = MockFilterChain()
        val request = Mockito.mock(ServletRequest::class.java)

        filter.doFilter(request, Mockito.mock(ServletResponse::class.java), chain)

        assertNull(KudosContextHolder.getOrNull())
    }

    @Test
    fun doFilter_clearsContextEvenWhenChainThrows() {
        val request = MockHttpServletRequest("GET", "/boom")
        val chain = object : MockFilterChain() {
            override fun doFilter(request: ServletRequest, response: ServletResponse) {
                assertTrue(KudosContextHolder.getOrNull() != null)
                throw IllegalStateException("downstream failure")
            }
        }

        val ex = assertFailsWith<IllegalStateException> {
            filter.doFilter(request, MockHttpServletResponse(), chain)
        }
        assertEquals("downstream failure", ex.message)
        assertNull(KudosContextHolder.getOrNull(), "context must be cleared in finally")
    }

}
