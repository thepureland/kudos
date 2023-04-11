package io.kudos.ability.cache.interservice.client.init

import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.cache.interservice.client.core.ClientCacheHelper
import org.soul.ability.cache.interservice.client.feign.FeignCacheRequestInterceptor
import org.soul.ability.cache.interservice.client.feign.FeignCacheResponseInterceptor
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.DependsOn
import javax.annotation.PostConstruct


/**
 * 服务间缓存客户端自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@AutoConfigureAfter(LinkableCacheAutoConfiguration::class)
open class InterServiceCacheClientAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @Bean("soulFeignCacheHelper")
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


    @PostConstruct
    override fun init() {
        logger.info("【kudos-ability-cache-interservice-client】初始化完成.")
    }

}