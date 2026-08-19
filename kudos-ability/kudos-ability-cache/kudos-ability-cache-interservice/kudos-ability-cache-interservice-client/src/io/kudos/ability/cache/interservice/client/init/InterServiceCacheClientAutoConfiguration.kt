package io.kudos.ability.cache.interservice.client.init

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.client.http.HttpCacheNegotiationInterceptor
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.HttpServiceGroupConfigurer

/**
 * Auto-configuration for inter-service cache negotiation on the client side.
 *
 * Replaced OpenFeign's two-bean setup (a `RequestInterceptor` plus a `@Primary` `feignDecoder`
 * wrapping Spring Cloud's decoder chain) with a single [HttpCacheNegotiationInterceptor] installed on
 * every HTTP service group. The decoder chain — `SpringDecoder` → `ResponseEntityDecoder` →
 * `OptionalDecoder` — has no counterpart and needs none: `RestClient` already runs the application's
 * `HttpMessageConverter`s, and `ResponseEntity` / `Optional` return types are handled by
 * `HttpServiceProxyFactory` itself.
 *
 * Overriding `feignDecoder` as `@Primary` was also the riskiest part of the old design: it replaced a
 * framework bean for the whole application in order to add one feature. Nothing is overridden here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(LinkableCacheAutoConfiguration::class)
@ConditionalOnClass(RestClientHttpServiceGroupConfigurer::class)
open class InterServiceCacheClientAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this::class)

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.interservice.client")
    open fun interServiceCacheClientProperties() = InterServiceCacheClientProperties()

    @Bean("interServiceCacheHelper")
    @ConditionalOnMissingBean
    open fun clientCacheHelper(
        properties: InterServiceCacheClientProperties,
        @Qualifier("localCacheManager") cacheManagerProvider: ObjectProvider<IKeyValueCacheManager<*>>
    ) = ClientCacheHelper(properties, cacheManagerProvider.ifAvailable)

    /**
     * Installs one negotiation interceptor per group, each told its own group name so that
     * `include-groups` / `exclude-groups` can select between them.
     *
     * Ordered last so the interceptor sees the URI after the LoadBalancer has resolved `lb://` — the
     * cache key includes the callee's scheme and authority, and an unresolved `lb://service` would key
     * every instance the same way only by accident of the placeholder host.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["interServiceCacheGroupConfigurer"])
    open fun interServiceCacheGroupConfigurer(
        cacheHelper: ClientCacheHelper,
        properties: InterServiceCacheClientProperties,
        @Value("\${spring.application.name:}") applicationName: String?,
    ): RestClientHttpServiceGroupConfigurer = object : RestClientHttpServiceGroupConfigurer {

        override fun configureGroups(groups: HttpServiceGroupConfigurer.Groups<RestClient.Builder>) {
            logger.info("Installing inter-service cache negotiation on HTTP service groups")
            groups.forEachClient(
                HttpServiceGroupConfigurer.ClientCallback<RestClient.Builder> { group, builder ->
                    builder.requestInterceptor(
                        HttpCacheNegotiationInterceptor(
                            cacheHelper = cacheHelper,
                            applicationName = applicationName,
                            includeGroups = properties.includeGroups,
                            excludeGroups = properties.excludeGroups,
                            group = group.name(),
                        )
                    )
                }
            )
        }

        override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
    }

    override fun getComponentName() = "kudos-ability-cache-interservice-client"
}
