package io.kudos.ability.cache.interservice.provider.web

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for CacheClientRequest
 *
 * Covers:
 * - servlet response defaults to null when omitted
 * - servlet response set/get round-trip and clearing back to null
 * - wrapper delegation to the underlying http request
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class CacheClientRequestTest {

    @Test
    fun servletResponse_defaultsToNull() {
        assertNull(CacheClientRequest(MockHttpServletRequest()).getServletResponse())
    }

    @Test
    fun servletResponse_setAndGetRoundTrip() {
        val request = CacheClientRequest(MockHttpServletRequest())
        val response = MockHttpServletResponse()

        request.setServletResponse(response)
        assertSame(response, request.getServletResponse())

        request.setServletResponse(null)
        assertNull(request.getServletResponse())
    }

    @Test
    fun constructor_acceptsResponseAndDelegatesToWrappedRequest() {
        val inner = MockHttpServletRequest().apply { addHeader("x-test", "value") }
        val response = MockHttpServletResponse()
        val request = CacheClientRequest(inner, response)

        assertSame(response, request.getServletResponse())
        assertEquals("value", request.getHeader("x-test"))
    }
}
