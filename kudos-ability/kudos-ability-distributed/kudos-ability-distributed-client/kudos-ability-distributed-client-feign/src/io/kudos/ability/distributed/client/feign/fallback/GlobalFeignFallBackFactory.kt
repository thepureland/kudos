package io.kudos.ability.distributed.client.feign.fallback

import io.kudos.base.logger.LogFactory
import io.kudos.base.net.http.HttpResult
import org.springframework.cloud.openfeign.FallbackFactory

/**
 * Global Feign fallback factory: maps exception types to HTTP status codes to avoid always returning 404.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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
