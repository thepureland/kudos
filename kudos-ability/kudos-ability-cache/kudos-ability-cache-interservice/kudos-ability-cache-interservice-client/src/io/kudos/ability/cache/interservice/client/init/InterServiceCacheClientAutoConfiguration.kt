package io.kudos.ability.cache.interservice.client.init

import feign.codec.Decoder
import feign.optionals.OptionalDecoder
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.client.feign.FeignCacheRequestInterceptor
import io.kudos.ability.cache.interservice.client.feign.FeignCacheResponseInterceptor
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import tools.jackson.databind.ObjectMapper

/**
 * 服务间缓存客户端自动配置
 */
@AutoConfigureAfter(LinkableCacheAutoConfiguration::class)
open class InterServiceCacheClientAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this)

    @Bean("feignCacheHelper")
    @ConditionalOnMissingBean
    open fun clientCacheHelper() = ClientCacheHelper()

    @Bean
    @ConditionalOnMissingBean
    open fun feignCacheRequestInterceptor() = FeignCacheRequestInterceptor()

    /**
     * 全局 Feign Decoder：
     *  - JacksonDecoder 负责真正反序列化
     *  - ResponseEntityDecoder 支持 ResponseEntity
     *  - OptionalDecoder 支持 Optional
     *  - 最外层 FeignCacheResponseInterceptor 加缓存能力
     */
    @Bean("feignDecoder")
    @Primary
    @ConditionalOnMissingBean(name = ["feignDecoder"])
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
