package io.kudos.ability.cache.interservice.client.init

import feign.RequestInterceptor
import feign.codec.Decoder
import feign.optionals.OptionalDecoder
import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.client.feign.FeignCacheRequestInterceptor
import io.kudos.ability.cache.interservice.client.feign.FeignCacheResponseInterceptor
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Auto-configuration for the inter-service cache client.
 *
 * `@Configuration` makes Spring treat this class as a full configuration class (CGLIB proxy +
 * cross bean-method calls keep single-instance semantics). Even though this class currently has no
 * inter-bean-method calls, using @Configuration is the safer form and avoids surprises when the
 * class is later refactored.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(LinkableCacheAutoConfiguration::class)
@ConditionalOnClass(RequestInterceptor::class)
open class InterServiceCacheClientAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this::class)

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.interservice.client")
    open fun interServiceCacheClientProperties() = InterServiceCacheClientProperties()

    @Bean("feignCacheHelper")
    @ConditionalOnMissingBean
    open fun clientCacheHelper(
        properties: InterServiceCacheClientProperties,
        @Qualifier("localCacheManager") cacheManagerProvider: ObjectProvider<IKeyValueCacheManager<*>>
    ) = ClientCacheHelper(properties, cacheManagerProvider.ifAvailable)

    @Bean
    @ConditionalOnMissingBean
    open fun feignCacheRequestInterceptor(
        cacheHelper: ClientCacheHelper,
        @Value("\${spring.application.name:}") applicationName: String?,
        properties: InterServiceCacheClientProperties,
    ) = FeignCacheRequestInterceptor(
        cacheHelper, applicationName, properties.includeClients, properties.excludeClients
    )

    /**
     * The converter set `SpringDecoder` decodes with.
     *
     * Spring Cloud declares this bean inside each Feign client's **child** context, so it is not reachable from
     * a decoder declared here in the parent — resolving it from the parent fails at decode time with
     * "No qualifying bean of type FeignHttpMessageConverters". Since the class builds its own converter list
     * from the two customizer providers rather than wrapping a pre-existing bean, constructing it here yields
     * the same set the child would have built, and `@ConditionalOnMissingBean` keeps an application-supplied
     * one authoritative.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun feignHttpMessageConverters(
        clientCustomizers: ObjectProvider<ClientHttpMessageConvertersCustomizer>,
        feignCustomizers: ObjectProvider<HttpMessageConverterCustomizer>
    ): FeignHttpMessageConverters = FeignHttpMessageConverters(clientCustomizers, feignCustomizers)

    /**
     * Global Feign `Decoder`: Spring Cloud's own chain, wrapped with the cache-negotiation layer.
     *
     * The inner chain is assembled exactly as `FeignClientsConfiguration.feignDecoder` does —
     * `SpringDecoder` over [FeignHttpMessageConverters], then `ResponseEntityDecoder`, then `OptionalDecoder` —
     * and only the outermost [FeignCacheResponseInterceptor] is ours.
     *
     * It used to put a bare Jackson decoder at the bottom instead, which **replaced** Spring Cloud's decoding
     * rather than decorating it. Because this bean is `@Primary` and lives in the parent context, Spring Cloud's
     * own `@ConditionalOnMissingBean` decoder never got created in any Feign client's child context — so merely
     * having this module on the classpath changed how *every* `@FeignClient` in the application deserialized:
     * custom `HttpMessageConverter`s (XML, protobuf, bespoke media types) were ignored, as were `@JsonView` and
     * anything contributed through Spring's Jackson configuration. The bare decoder also mapped 404 to null,
     * swallowing the error, and hardcoded UTF-8 instead of honouring `response.charset()`.
     *
     * Note that this bean still *suppresses* Spring Cloud's decoder bean rather than wrapping the instance —
     * that is unavoidable, since the two are mutually exclusive by construction (`@ConditionalOnMissingBean`
     * searches ancestor contexts). Building the identical chain here is what keeps the behaviour equivalent.
     */
    @Bean("feignDecoder")
    @Primary
    @ConditionalOnMissingBean(name = ["feignDecoder"])
    @ConditionalOnProperty(
        prefix = "kudos.ability.cache.interservice.client",
        name = ["decoder-enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun feignDecoder(
        messageConverters: ObjectProvider<FeignHttpMessageConverters>,
        cacheHelper: ClientCacheHelper
    ): Decoder {
        logger.info("Init FeignCacheResponseInterceptor over Spring Cloud's SpringDecoder (HttpMessageConverters preserved)")

        val springDecoder: Decoder = SpringDecoder(messageConverters)
        val responseEntityDecoder: Decoder = ResponseEntityDecoder(springDecoder)
        val optionalDecoder: Decoder = OptionalDecoder(responseEntityDecoder)

        return FeignCacheResponseInterceptor(optionalDecoder, cacheHelper)
    }

    override fun getComponentName() = "kudos-ability-cache-interservice-client"
}
