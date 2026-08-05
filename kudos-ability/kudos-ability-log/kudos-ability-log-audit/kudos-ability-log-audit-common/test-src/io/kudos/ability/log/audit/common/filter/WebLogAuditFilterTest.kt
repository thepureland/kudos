package io.kudos.ability.log.audit.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [WebLogAuditFilter].
 *
 * Coverage:
 *  - The downstream filter chain receives a [ContentCachingRequestWrapper], not the raw request.
 *  - After the business side consumes the input stream, the body remains replayable via
 *    `contentAsByteArray` (the audit aspect's read path) — pins the contentCacheLimit fix:
 *    the historical `ContentCachingRequestWrapper(request, 0)` cached 0 bytes and lost every body.
 *  - Constants: bean name and the 10 MB cache limit.
 *
 * @author K
 * @since 1.0.0
 */
internal class WebLogAuditFilterTest {

    @Test
    fun doFilter_wrapsRequestWithContentCachingWrapper_andBodyIsReplayable() {
        val body = """{"name":"愛麗絲","phone":"13800001234"}"""
        val request = MockHttpServletRequest("POST", "/api/users").apply {
            contentType = "application/json;charset=UTF-8"
            setContent(body.toByteArray(Charsets.UTF_8))
        }
        val response = MockHttpServletResponse()

        var seenRequest: ServletRequest? = null
        val chain = FilterChain { req: ServletRequest, _: ServletResponse ->
            seenRequest = req
            // Simulate the business side consuming the request body once
            req.inputStream.readAllBytes()
        }

        WebLogAuditFilter().doFilter(request, response, chain)

        val wrapper = assertIs<ContentCachingRequestWrapper>(assertNotNull(seenRequest),
            "downstream must see a ContentCachingRequestWrapper so the audit aspect can replay the body")
        assertEquals(body, String(wrapper.contentAsByteArray, Charsets.UTF_8),
            "the consumed body must remain replayable from the cache")
    }

    @Test
    fun constants_beanNameAndCacheLimit() {
        assertEquals("webLogAuditFilter", WebLogAuditFilter.BEAN_NAME)
        assertEquals(10 * 1024 * 1024, WebLogAuditFilter.CONTENT_CACHE_LIMIT_BYTES,
            "cache limit is 10 MB — bounds per-request memory while covering realistic audited payloads")
    }
}
