package io.kudos.ability.distributed.client.http.fallback

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [HttpFallbackStatusResolver] — the interface-client counterpart of
 * `FeignFallbackStatusResolverTest`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class HttpFallbackStatusResolverTest {

    @Test
    fun clientErrorStatusIsPassedThrough() {
        assertEquals(404, HttpFallbackStatusResolver.resolve(HttpClientErrorException(HttpStatus.NOT_FOUND)))
    }

    @Test
    fun serverErrorStatusIsPassedThrough() {
        assertEquals(502, HttpFallbackStatusResolver.resolve(HttpServerErrorException(HttpStatus.BAD_GATEWAY)))
    }

    @Test
    fun nestedResponseExceptionIsFound() {
        val nested = RuntimeException("wrapper", HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
        assertEquals(503, HttpFallbackStatusResolver.resolve(nested))
    }

    @Test
    fun responseStatusWinsOverTransportClassification() {
        // A response status anywhere in the chain is more specific than the transport-level guess,
        // so it must be checked first even when a timeout also appears further down.
        val nested = HttpClientErrorException(HttpStatus.BAD_REQUEST).apply {
            initCause(SocketTimeoutException("read timed out"))
        }
        assertEquals(400, HttpFallbackStatusResolver.resolve(nested))
    }

    @Test
    fun socketTimeoutMapsTo504_evenWhenWrappedByResourceAccessException() {
        // The case a top-level-only `when` would get wrong: RestClient never surfaces a bare
        // SocketTimeoutException, it always arrives wrapped in ResourceAccessException.
        val cause = ResourceAccessException("I/O error", SocketTimeoutException("read timed out"))
        assertEquals(504, HttpFallbackStatusResolver.resolve(cause))
    }

    @Test
    fun timeoutExceptionMapsTo504() {
        assertEquals(504, HttpFallbackStatusResolver.resolve(TimeoutException("timed out")))
    }

    @Test
    fun connectExceptionMapsTo503_evenWhenWrapped() {
        val cause = ResourceAccessException("I/O error", ConnectException("connection refused"))
        assertEquals(503, HttpFallbackStatusResolver.resolve(cause))
    }

    @Test
    fun unknownExceptionMapsTo503() {
        assertEquals(503, HttpFallbackStatusResolver.resolve(IllegalStateException("boom")))
    }

    @Test
    fun selfReferencingCauseChainDoesNotLoop() {
        val cause = object : RuntimeException("cyclic") {
            override val cause: Throwable get() = this
        }
        assertEquals(503, HttpFallbackStatusResolver.resolve(cause))
    }
}
