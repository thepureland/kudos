package io.kudos.ability.distributed.client.feign.fallback

import io.kudos.base.logger.LogFactory
import io.kudos.base.net.http.HttpResult
import org.springframework.cloud.openfeign.FallbackFactory

/**
 * 全局Feign降级工厂
 * 当Feign调用失败时提供统一的降级处理，返回404错误结果
 */
class GlobalFeignFallBackFactory : FallbackFactory<HttpResult> {

    override fun create(cause: Throwable): HttpResult {
        val result = HttpResult(404, cause.message!!)
        log.debug("Hystrix FallBackFactory -----------------------------------------------------------")
        return result
    }

    private val log = LogFactory.getLog(this)
}