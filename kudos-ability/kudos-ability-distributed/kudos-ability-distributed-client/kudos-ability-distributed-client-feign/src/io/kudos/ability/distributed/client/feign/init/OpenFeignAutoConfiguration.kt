package io.kudos.ability.distributed.client.feign.init

import feign.RequestInterceptor
import io.kudos.ability.distributed.client.feign.fallback.GlobalFeignFallBackFactory
import io.kudos.ability.distributed.client.feign.interceptor.GlobalHeaderRequestInterceptor
import io.kudos.ability.distributed.client.feign.init.properties.OpenFeignProperties
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * OpenFeign auto-configuration class.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class OpenFeignAutoConfiguration : IComponentInitializer {

    /**
     * Global Feign request interceptor: automatically injects the kudos context (tenantId / subSysCode /
     * traceKey, etc.) into every Feign request header. See [GlobalHeaderRequestInterceptor] for details.
     */
    @Bean("globalHeaderRequestInterceptor")
    open fun feignCacheRequestInterceptor(properties: OpenFeignProperties): RequestInterceptor =
        GlobalHeaderRequestInterceptor(properties)

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.client.feign")
    open fun openFeignProperties() = OpenFeignProperties()

    /**
     * Global Feign fallback factory: maps exception types to HTTP status codes, intended for use with
     * `@FeignClient(fallbackFactory = ...)`.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun globalFeignFallBackFactory() = GlobalFeignFallBackFactory()

    override fun getComponentName() = "kudos-ability-distributed-client-feign"

}
