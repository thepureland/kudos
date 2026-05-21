package io.kudos.ability.distributed.client.feign.fallback

import io.kudos.base.logger.LogFactory
import io.kudos.base.net.http.HttpResult
import org.springframework.cloud.openfeign.FallbackFactory

/**
 * 全局 Feign 降级工厂：按异常类型映射 HTTP 状态码，避免一律返回 404。
 */
class GlobalFeignFallBackFactory : FallbackFactory<HttpResult> {

    private val log = LogFactory.getLog(this::class)

    override fun create(cause: Throwable): HttpResult {
        val status = FeignFallbackStatusResolver.resolve(cause)
        val msg = cause.message ?: cause.toString()
        log.debug("Feign fallback, status={0}, type={1}", status, cause.javaClass.name)
        return HttpResult(status, msg)
    }
}
