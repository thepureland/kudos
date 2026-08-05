package io.kudos.ability.web.springmvc.filter

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.support.Consts
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
