package io.kudos.ability.web.springmvc.interceptor

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CorsHandlerInterceptor].
 *
 * Covers: CORS response-header injection with and without an Origin request header,
 * and the always-true return value of preHandle.
 *
 * @author K
 * @since 1.0.0
 */
internal class CorsHandlerInterceptorTest {

    private val interceptor = CorsHandlerInterceptor()

    @Test
    fun preHandle_reflectsOriginAndSetsCorsHeaders() {
        val request = MockHttpServletRequest().apply { addHeader("Origin", "https://kudos.example") }
        val response = MockHttpServletResponse()

        val proceed = interceptor.preHandle(request, response, Any())

        assertTrue(proceed)
        assertEquals("https://kudos.example", response.getHeader("Access-Control-Allow-Origin"))
        assertEquals("*", response.getHeader("Access-Control-Allow-Methods"))
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"))
        assertEquals(
            "Authorization,Origin, X-Requested-With, Content-Type, Accept,Access-Token",
            response.getHeader("Access-Control-Allow-Headers")
        )
    }

    @Test
    fun preHandle_withoutOrigin_setsNoAllowOriginHeader() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        val proceed = interceptor.preHandle(request, response, Any())

        assertTrue(proceed)
        // Reflecting a missing Origin yields a null header value (i.e. the header is not set).
        assertNull(response.getHeader("Access-Control-Allow-Origin"))
        assertEquals("*", response.getHeader("Access-Control-Allow-Methods"))
    }

}
