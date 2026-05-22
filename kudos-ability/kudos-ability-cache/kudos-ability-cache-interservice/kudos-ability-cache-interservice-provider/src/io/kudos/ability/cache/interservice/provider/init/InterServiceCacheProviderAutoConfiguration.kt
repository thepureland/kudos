package io.kudos.ability.cache.interservice.provider.init

import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.interservice.aop.ClientCacheableAspect
import io.kudos.ability.cache.interservice.aop.ClientCacheUidGenerator
import io.kudos.ability.cache.interservice.provider.web.ClientCacheWebFilter
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * 服务间缓存服务端自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(LinkableCacheAutoConfiguration::class)
open class InterServiceCacheProviderAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.interservice.provider")
    open fun interServiceCacheProviderProperties() = InterServiceCacheProviderProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun clientCacheWebFilter(
        properties: InterServiceCacheProviderProperties
    ): FilterRegistrationBean<ClientCacheWebFilter> {
        val registration = FilterRegistrationBean<ClientCacheWebFilter>()
        //注入过滤器
        registration.setFilter(ClientCacheWebFilter(properties.wrapAllRequests))
        //拦截规则
        registration.addUrlPatterns("/*")
        //过滤器名称
        registration.setName("clientCacheWebFilter")
        //过滤器顺序
        registration.order = FilterRegistrationBean.HIGHEST_PRECEDENCE
        return registration
    }

    @Bean
    @ConditionalOnMissingBean
    open fun clientCacheUidGenerator(
        properties: InterServiceCacheProviderProperties
    ) = ClientCacheUidGenerator(properties.uidCacheEnabled)

    @Bean
    @ConditionalOnMissingBean
    open fun clientCacheableAspect(uidGenerator: ClientCacheUidGenerator) = ClientCacheableAspect(uidGenerator)

    override fun getComponentName() = "kudos-ability-cache-interservice-provider"

}
