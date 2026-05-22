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
 * OpenFeign自动配置类
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class OpenFeignAutoConfiguration : IComponentInitializer {

    /**
     * 全局 Feign 请求拦截器——把 kudos 上下文（tenantId / subSysCode / traceKey 等）
     * 自动塞进每个 Feign 请求头。详细行为见 [GlobalHeaderRequestInterceptor]。
     */
    @Bean("globalHeaderRequestInterceptor")
    open fun feignCacheRequestInterceptor(properties: OpenFeignProperties): RequestInterceptor =
        GlobalHeaderRequestInterceptor(properties)

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.client.feign")
    open fun openFeignProperties() = OpenFeignProperties()

    /**
     * 全局 Feign 降级工厂：按异常类型映射 HTTP 状态码，配合 `@FeignClient(fallbackFactory = ...)` 使用。
     */
    @Bean
    @ConditionalOnMissingBean
    open fun globalFeignFallBackFactory() = GlobalFeignFallBackFactory()

    override fun getComponentName() = "kudos-ability-distributed-client-feign"

}
