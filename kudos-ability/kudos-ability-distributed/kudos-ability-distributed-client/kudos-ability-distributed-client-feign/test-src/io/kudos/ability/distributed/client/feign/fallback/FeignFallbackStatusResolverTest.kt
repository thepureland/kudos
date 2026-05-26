package io.kudos.ability.distributed.client.feign.fallback

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [FeignFallbackStatusResolver] status code mapping tests.
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
}
