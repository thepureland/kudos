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
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tools.jackson.databind.ObjectMapper

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
    ) = FeignCacheRequestInterceptor(cacheHelper, applicationName)

    /**
     * Global Feign Decoder:
     *  - JacksonDecoder performs the actual deserialization
     *  - ResponseEntityDecoder adds ResponseEntity support
     *  - OptionalDecoder adds Optional support
     *  - The outermost FeignCacheResponseInterceptor adds caching capability
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
        objectMapper: ObjectMapper,
        cacheHelper: ClientCacheHelper
    ): Decoder {
        logger.info("Init FeignCacheResponseInterceptor (Jackson based, no HttpMessageConverters)")

        val jacksonDecoder: Decoder = JacksonDecoder(objectMapper)
        val responseEntityDecoder: Decoder = ResponseEntityDecoder(jacksonDecoder)
        val optionalDecoder: Decoder = OptionalDecoder(responseEntityDecoder)

        return FeignCacheResponseInterceptor(optionalDecoder, cacheHelper)
    }

    override fun getComponentName() = "kudos-ability-cache-interservice-client"
}
