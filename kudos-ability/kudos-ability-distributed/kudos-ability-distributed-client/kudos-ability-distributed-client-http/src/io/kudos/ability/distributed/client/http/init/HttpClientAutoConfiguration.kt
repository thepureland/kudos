package io.kudos.ability.distributed.client.http.init

import io.kudos.ability.distributed.client.http.init.properties.HttpClientProperties
import io.kudos.ability.distributed.client.http.interceptor.KudosContextRequestInterceptor
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.HttpServiceGroupConfigurer

/**
 * Auto-configuration for kudos interface clients (Spring HTTP Service Clients).
 *
 * The counterpart of `OpenFeignAutoConfiguration`. Discovered the same way — implementing
 * [IComponentInitializer] under `io.kudos.ability` is enough for `ComponentInitializerSelector` to
 * import it.
 *
 * What it wires up:
 * - [KudosContextRequestInterceptor] as a bean, and
 * - a [RestClientHttpServiceGroupConfigurer] that installs that interceptor on **every** registered
 *   HTTP service group, which is what makes context propagation global the way Feign's
 *   `RequestInterceptor` bean was.
 *
 * What it deliberately does **not** wire up:
 * - service discovery / load balancing — `LoadBalancerRestClientHttpServiceGroupConfigurer`
 *   (spring-cloud-commons) already does it for any group whose base-url uses the `lb://` scheme;
 * - fallbacks — `CircuitBreakerRestClientHttpServiceGroupConfigurer` (also spring-cloud-commons)
 *   handles `@HttpServiceFallback`, provided a `CircuitBreakerFactory` bean exists (this module pulls
 *   in the resilience4j starter for exactly that reason).
 *
 * There is therefore no equivalent of `GlobalFeignFallBackFactory` here: Feign needed a hand-written
 * factory because it had no circuit breaker of its own, whereas the interface-client path routes
 * every call through Spring Cloud CircuitBreaker and picks the fallback bean per service. What
 * survives from the Feign module is [io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport]
 * (logging conventions) and [io.kudos.ability.distributed.client.http.fallback.HttpFallbackStatusResolver]
 * (exception -> status classification), both usable from any `@HttpServiceFallback` bean.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class HttpClientAutoConfiguration : IComponentInitializer {

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.client.http")
    open fun kudosHttpClientProperties() = HttpClientProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun kudosContextRequestInterceptor(properties: HttpClientProperties) =
        KudosContextRequestInterceptor(properties)

    /**
     * Installs the context interceptor on every group's `RestClient.Builder`.
     *
     * Ordering against the LoadBalancer and CircuitBreaker configurers is intentionally left at
     * [Ordered.LOWEST_PRECEDENCE]: the signed payload uses the request path only, which no other
     * configurer rewrites, so this interceptor is order-independent.
     */
    @Bean
    open fun kudosContextHttpServiceGroupConfigurer(
        interceptor: KudosContextRequestInterceptor
    ): RestClientHttpServiceGroupConfigurer = object : RestClientHttpServiceGroupConfigurer {

        override fun configureGroups(groups: HttpServiceGroupConfigurer.Groups<RestClient.Builder>) {
            groups.forEachClient(
                HttpServiceGroupConfigurer.ClientCallback<RestClient.Builder> { _, builder ->
                    builder.requestInterceptor(interceptor)
                }
            )
        }

        override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
    }

    override fun getComponentName() = "kudos-ability-distributed-client-http"

}
