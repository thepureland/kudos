package io.kudos.ability.distributed.client.feign.fallback

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [FeignFallbackStatusResolver] status code mapping tests.
 *
 * Coverage:
 *  - FeignException with a valid status (100..599, incl. boundaries) wins, whether at the top of
 *    the chain or nested deeper as a cause.
 *  - FeignException with an out-of-range status (0 / 600) is ignored and falls through to the
 *    type-based mapping; a deeper valid FeignException in the same chain still wins.
 *  - Timeout exceptions -> 504; ConnectException / unknown -> 503.
 *  - Type-based mapping only inspects the top-level exception (a wrapped timeout maps to 503).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class FeignFallbackStatusResolverTest {

    @Test
    fun resolve_timeoutExceptionsAsGatewayTimeout() {
        assertEquals(504, FeignFallbackStatusResolver.resolve(SocketTimeoutException("timeout")))
        assertEquals(504, FeignFallbackStatusResolver.resolve(TimeoutException("timeout")))
    }

    @Test
    fun resolve_connectExceptionAsServiceUnavailable() {
        assertEquals(503, FeignFallbackStatusResolver.resolve(ConnectException("refused")))
    }

    @Test
    fun resolve_unknownExceptionAsServiceUnavailable() {
        assertEquals(503, FeignFallbackStatusResolver.resolve(IllegalStateException("boom")))
    }

    @Test
    fun resolve_topLevelFeignExceptionStatusWins() {
        assertEquals(404, FeignFallbackStatusResolver.resolve(TestFeignException(404)))
        assertEquals(500, FeignFallbackStatusResolver.resolve(TestFeignException(500)))
    }

    @Test
    fun resolve_feignExceptionStatusBoundaries() {
        assertEquals(100, FeignFallbackStatusResolver.resolve(TestFeignException(100)))
        assertEquals(599, FeignFallbackStatusResolver.resolve(TestFeignException(599)))
    }

    @Test
    fun resolve_nestedFeignExceptionInCauseChainWins() {
        val nested = RuntimeException("wrapper", IllegalStateException(TestFeignException(429)))
        assertEquals(429, FeignFallbackStatusResolver.resolve(nested))
    }

    @Test
    fun resolve_feignExceptionWithOutOfRangeStatus_fallsThroughToTypeMapping() {
        // status 0 (unreachable) and 600 are outside 100..599 and must be ignored
        assertEquals(503, FeignFallbackStatusResolver.resolve(TestFeignException(0)))
        assertEquals(503, FeignFallbackStatusResolver.resolve(TestFeignException(600)))
    }

    @Test
    fun resolve_outOfRangeFeignException_butDeeperValidFeignExceptionWins() {
        val chain = RuntimeException(java.lang.RuntimeException(TestFeignException(0)))
        assertEquals(503, FeignFallbackStatusResolver.resolve(chain))

        // first FeignException in the chain has invalid status; a deeper one carries 418
        val outer = object : feign.FeignException(0, "no status", TestFeignException(418)) {}
        assertEquals(418, FeignFallbackStatusResolver.resolve(outer))
    }

    @Test
    fun resolve_wrappedTimeout_isNotUnwrapped_byTypeMapping() {
        // Type-based mapping only checks the top-level exception: a RuntimeException wrapping a
        // SocketTimeoutException resolves to the generic 503, not 504.
        val wrapped = RuntimeException("wrapper", SocketTimeoutException("timeout"))
        assertEquals(503, FeignFallbackStatusResolver.resolve(wrapped))
    }
}
