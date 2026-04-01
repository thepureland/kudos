package io.kudos.ability.distributed.client.feign.fallback

import feign.FeignException
import io.kudos.base.logger.LogFactory
import io.kudos.base.net.http.HttpResult
import org.springframework.cloud.openfeign.FallbackFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * 全局 Feign 降级工厂：按异常类型映射 HTTP 状态码，避免一律返回 404。
 */
class GlobalFeignFallBackFactory : FallbackFactory<HttpResult> {

    private val log = LogFactory.getLog(this::class)

    override fun create(cause: Throwable): HttpResult {
        val status = resolveHttpStatus(cause)
        val msg = cause.message ?: cause.toString()
        log.debug("Feign fallback, status={0}, type={1}", status, cause.javaClass.name)
        return HttpResult(status, msg)
    }

    private fun resolveHttpStatus(cause: Throwable): Int {
        var t: Throwable? = cause
        while (t != null) {
            if (t is FeignException) {
                val s = t.status()
                if (s in 100..599) {
                    return s
                }
            }
            t = t.cause
        }
        return when (cause) {
            is SocketTimeoutException, is TimeoutException -> 504
            is ConnectException -> 503
            else -> 503
        }
    }
}
