package io.kudos.ability.distributed.client.feign.fallback

import feign.FeignException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Status-code resolver for Feign fallbacks.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object FeignFallbackStatusResolver {

    /**
     * Maps a Feign exception to the HTTP status code used for the fallback.
     */
    fun resolve(cause: Throwable): Int {
        generateSequence(cause as Throwable?) { it.cause }
            .filterIsInstance<FeignException>()
            .map { it.status() }
            .firstOrNull { it in 100..599 }
            ?.let { return it }
        return when (cause) {
            is SocketTimeoutException, is TimeoutException -> 504
            is ConnectException -> 503
            else -> 503
        }
    }
}
