package io.kudos.ability.distributed.client.http.fallback

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.ConnectException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [AbstractHttpFallbackSupport.describeStatus] — the pure classification logic that
 * decides how a degraded call is labelled in the log.
 *
 * The Feign module's equivalent (`AbstractFeignFallbackSupport.describeStatus`) has no coverage at
 * all; this closes that gap on the interface-client side rather than porting the hole across.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AbstractHttpFallbackSupportTest {

    private val support = object : AbstractHttpFallbackSupport("TestFallback") {}

    @Test
    fun nullCauseIsUnknown() {
        assertEquals("unknown", support.describeStatus(null))
    }

    @Test
    fun clientErrorIsLabelledWithStatus() {
        assertEquals("client-error-404", support.describeStatus(HttpClientErrorException(HttpStatus.NOT_FOUND)))
    }

    @Test
    fun serverErrorIsLabelledWithStatus() {
        assertEquals(
            "server-error-500",
            support.describeStatus(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
        )
    }

    @Test
    fun nestedResponseExceptionIsStillClassified() {
        val nested = RuntimeException("wrapper", HttpClientErrorException(HttpStatus.FORBIDDEN))
        assertEquals("client-error-403", support.describeStatus(nested))
    }

    @Test
    fun resourceAccessExceptionIsUnreachable() {
        val cause = ResourceAccessException("I/O error", ConnectException("connection refused"))
        assertEquals("unreachable", support.describeStatus(cause))
    }

    @Test
    fun otherExceptionsAreLabelledByType() {
        assertEquals("exception-IllegalStateException", support.describeStatus(IllegalStateException("boom")))
    }
}
