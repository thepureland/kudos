package io.kudos.ability.distributed.client.feign.fallback

import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [GlobalFeignFallBackFactory].
 *
 * Coverage:
 *  - create() maps FeignException status straight through (4xx / 5xx).
 *  - create() maps timeout exceptions to 504 and connect/unknown exceptions to 503
 *    (delegation to [FeignFallbackStatusResolver]).
 *  - msg uses the exception message when present, otherwise falls back to cause.toString()
 *    (null message branch).
 *  - HttpResult.data stays null (factory carries no payload).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class GlobalFeignFallBackFactoryTest {

    private val factory = GlobalFeignFallBackFactory()

    @Test
    fun create_feignException_passesStatusAndMessageThrough() {
        val result = factory.create(TestFeignException(404, "not found"))
        assertEquals(404, result.status)
        assertEquals("not found", result.msg)
        assertEquals(null, result.data)
    }

    @Test
    fun create_feignServerError_passesStatusThrough() {
        val result = factory.create(TestFeignException(502, "bad gateway"))
        assertEquals(502, result.status)
        assertEquals("bad gateway", result.msg)
    }

    @Test
    fun create_timeout_mapsTo504() {
        val result = factory.create(SocketTimeoutException("read timed out"))
        assertEquals(504, result.status)
        assertEquals("read timed out", result.msg)
    }

    @Test
    fun create_connectException_mapsTo503() {
        val result = factory.create(ConnectException("connection refused"))
        assertEquals(503, result.status)
        assertEquals("connection refused", result.msg)
    }

    @Test
    fun create_nullMessage_fallsBackToToString() {
        val cause = IllegalStateException()
        val result = factory.create(cause)
        assertEquals(503, result.status)
        assertEquals(cause.toString(), result.msg)
        assertEquals("java.lang.IllegalStateException", result.msg)
    }

    @Test
    fun create_unicodeMessage_isPreserved() {
        val result = factory.create(RuntimeException("遠端服務不可用-ünïcødé"))
        assertEquals(503, result.status)
        assertEquals("遠端服務不可用-ünïcødé", result.msg)
    }
}
