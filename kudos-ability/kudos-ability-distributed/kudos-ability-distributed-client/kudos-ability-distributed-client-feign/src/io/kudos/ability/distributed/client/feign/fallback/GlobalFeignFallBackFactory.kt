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

    /**
     * 把 Feign 异常映射为 HTTP 状态码用于 fallback。
     *
     * 沿 cause 链向上找首个携合法状态码 (100..599) 的 [FeignException]；找不到则按类型粗分：
     * - [SocketTimeoutException]/[TimeoutException] → 504（网关超时）
     * - [ConnectException] → 503（连不上下游）
     * - 其它 → 503
     *
     * @param cause Feign 抛出的根因
     * @return 对应的 HTTP 状态码
     * @author K
     * @since 1.0.0
     */
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
