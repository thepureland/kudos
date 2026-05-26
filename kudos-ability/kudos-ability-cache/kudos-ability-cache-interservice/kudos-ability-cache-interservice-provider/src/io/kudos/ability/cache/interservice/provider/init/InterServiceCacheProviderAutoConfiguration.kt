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
 * Auto-configuration for the inter-service cache provider (server) side.
 *
 * @author K
 * @author AI: Codex
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
        // Inject the filter
        registration.setFilter(ClientCacheWebFilter(properties.wrapAllRequests))
        // URL patterns
        registration.addUrlPatterns("/*")
        // Filter name
        registration.setName("clientCacheWebFilter")
        // Filter order
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
