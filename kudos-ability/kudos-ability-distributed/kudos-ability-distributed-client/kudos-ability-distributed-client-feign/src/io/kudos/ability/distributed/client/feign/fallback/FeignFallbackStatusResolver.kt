package io.kudos.ability.distributed.client.feign.fallback

import feign.FeignException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Feign fallback 状态码解析器。
 */
object FeignFallbackStatusResolver {

    /**
     * 把 Feign 异常映射为 HTTP 状态码用于 fallback。
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
