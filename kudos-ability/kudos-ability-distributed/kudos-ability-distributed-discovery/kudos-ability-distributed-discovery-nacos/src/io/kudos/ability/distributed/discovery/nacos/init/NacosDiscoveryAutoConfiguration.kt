package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter
import io.kudos.ability.distributed.discovery.nacos.init.properties.NacosDiscoveryProperties
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Nacos service-discovery assembly entry point.
 *
 * The actual Nacos discovery client wiring is delegated to the `alibaba.cloud.nacos.discovery`
 * starter; this class only adds two kudos-specific pieces:
 *
 *  1. Via [getComponentName] hooks this module into the kudos-custom SPI dispatcher
 *     `ComponentInitializerSelector`.
 *  2. Registers [FeignContextWebFilter] — the sole entry point that **writes headers propagated
 *     by Feign clients (`TENANT_ID` / `TRACE_KEY` / `DATASOURCE_ID` etc.) back into the
 *     `KudosContext` on the provider side**.
 *
 * Filter registration details:
 *  - Only assembled when `FilterRegistrationBean` is available ([ConditionalOnClass]); does not
 *    affect non-servlet applications.
 *  - Kill switch `kudos.ability.distributed.discovery.nacos.feign-context-filter.enabled=false`
 *    for dev debugging or transitional scenarios that integrate non-kudos clients.
 *  - Order set as early as possible ([FilterRegistrationBean.HIGHEST_PRECEDENCE] + 1) — the
 *    context must be ready before business filters / interceptors, otherwise downstream code
 *    sees an empty `KudosContextHolder`.
 *  - urlPatterns covers all paths (`/&#42;`) — the filter internally uses explicit
 *    `FEIGN_REQUEST` / `NOTIFY_REQUEST` markers to block regular browser / curl requests, so no
 *    path whitelisting is needed at the registration layer.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosDiscoveryAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-distributed-discovery-nacos"

    @Bean
    @ConditionalOnClass(FilterRegistrationBean::class)
    @ConditionalOnProperty(
        prefix = "kudos.ability.distributed.discovery.nacos.feign-context-filter",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun feignContextWebFilterRegistration(
        properties: NacosDiscoveryProperties = NacosDiscoveryProperties()
    ): FilterRegistrationBean<FeignContextWebFilter> {
        val registration = FilterRegistrationBean<FeignContextWebFilter>()
        registration.setFilter(FeignContextWebFilter(properties.feignContextFilter.allowUnmarkedContextHeaders))
        registration.addUrlPatterns("/*")
        registration.order = NacosDiscoveryProperties.FILTER_ORDER
        registration.setName("feignContextWebFilter")
        return registration
    }

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.discovery.nacos")
    open fun nacosDiscoveryProperties() = NacosDiscoveryProperties()

}
