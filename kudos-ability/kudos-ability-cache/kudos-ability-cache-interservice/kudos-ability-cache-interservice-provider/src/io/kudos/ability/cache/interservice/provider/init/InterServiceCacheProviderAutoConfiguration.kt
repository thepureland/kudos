package io.kudos.ability.cache.interservice.provider.init

import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.cache.interservice.provider.ClientCacheableAspect
import org.soul.ability.cache.interservice.provider.web.ClientCacheWebFilter
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import javax.annotation.PostConstruct


/**
 * 服务间缓存服务端自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class InterServiceCacheProviderAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @Bean
    @ConditionalOnMissingBean
    open fun clientCacheWebFilter(): FilterRegistrationBean<ClientCacheWebFilter> {
        val registration = FilterRegistrationBean<ClientCacheWebFilter>()
        //注入过滤器
        registration.setFilter(ClientCacheWebFilter())
        //拦截规则
        registration.addUrlPatterns("/*")
        //过滤器名称
        registration.setName("clientCacheWebFilter")
        //过滤器顺序
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE)
        return registration
    }

    @Bean
    @ConditionalOnMissingBean
    open fun clientCacheableAspect() = ClientCacheableAspect()

    @PostConstruct
    override fun init() {
        logger.info("【kudos-ability-cache-interservice-provider】初始化完成.")
    }

}