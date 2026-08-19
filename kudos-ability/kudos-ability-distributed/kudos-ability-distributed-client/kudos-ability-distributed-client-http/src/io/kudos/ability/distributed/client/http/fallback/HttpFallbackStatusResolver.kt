package io.kudos.ability.distributed.client.http.fallback

import org.springframework.web.client.RestClientResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Status-code resolver for interface-client fallbacks.
 *
 * The counterpart of `FeignFallbackStatusResolver`, with `FeignException` replaced by
 * [RestClientResponseException].
 *
 * One deliberate behavior difference from the Feign version: timeout / connect exceptions are looked
 * up **through the whole cause chain** rather than only on the top-level exception. `RestClient`
 * always wraps low-level IO failures in `ResourceAccessException`, so a top-level-only `when` (which
 * is what the Feign resolver does) would classify every timeout as the generic 503 and never produce
 * 504.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object HttpFallbackStatusResolver {

    /**
     * Maps an interface-client exception to the HTTP status code used for the fallback.
     */
    fun resolve(cause: Throwable): Int {
        val chain = causeChain(cause)
        chain.filterIsInstance<RestClientResponseException>()
            .map { it.statusCode.value() }
            .firstOrNull { it in 100..599 }
            ?.let { return it }
        chain.forEach {
            when (it) {
                is SocketTimeoutException, is TimeoutException -> return 504
                is ConnectException -> return 503
            }
        }
        return 503
    }

    /**
     * The exception and its causes, guarding against self-referencing / cyclic `cause` chains.
     */
    internal fun causeChain(cause: Throwable): List<Throwable> {
        val chain = mutableListOf<Throwable>()
        var current: Throwable? = cause
        while (current != null && chain.none { it === current }) {
            chain += current
            current = current.cause
        }
        return chain
    }
}
