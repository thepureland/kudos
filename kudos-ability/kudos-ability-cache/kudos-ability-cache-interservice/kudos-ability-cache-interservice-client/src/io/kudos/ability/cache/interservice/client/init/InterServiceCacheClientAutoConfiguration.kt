package io.kudos.ability.cache.interservice.client.init

import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.client.feign.FeignCacheRequestInterceptor
import io.kudos.ability.cache.interservice.client.feign.FeignCacheResponseInterceptor
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer
import org.springframework.context.annotation.Bean


/**
 * 服务间缓存客户端自动配置类
 *
 * @author K
 * @since 1.0.0
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

    @Bean
    @ConditionalOnMissingBean
    open fun feignCacheResponseInterceptor(
        messageConverters: ObjectFactory<HttpMessageConverters>,
        customizers: ObjectProvider<HttpMessageConverterCustomizer>
    ) = FeignCacheResponseInterceptor(messageConverters, customizers)

    override fun getComponentName() = "kudos-ability-cache-interservice-client"

}