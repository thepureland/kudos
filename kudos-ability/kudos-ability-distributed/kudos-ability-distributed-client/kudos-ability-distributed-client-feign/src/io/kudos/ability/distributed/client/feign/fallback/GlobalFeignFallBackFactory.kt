package io.kudos.ability.distributed.client.feign.fallback

import io.kudos.base.logger.LogFactory
import io.kudos.base.net.http.HttpResult
import org.springframework.cloud.openfeign.FallbackFactory

class GlobalFeignFallBackFactory : FallbackFactory<HttpResult> {

    override fun create(cause: Throwable): HttpResult {
        val result = HttpResult(404, cause.message!!)
        LOG.debug("Hystrix FallBackFactory -----------------------------------------------------------")
        return result
    }

    private val LOG = LogFactory.getLog(this)
}