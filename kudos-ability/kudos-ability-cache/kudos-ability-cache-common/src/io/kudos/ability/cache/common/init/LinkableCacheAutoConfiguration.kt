package io.kudos.ability.cache.common.init

import io.kudos.base.logger.LogFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import org.soul.ability.cache.common.CacheHandlerBeanPostProcessor
import org.soul.ability.cache.common.MixCacheInitializing
import org.soul.ability.cache.common.MixCacheManager
import org.soul.ability.cache.common.batch.BatchCacheableAspect
import org.soul.ability.cache.common.notify.CacheNotifyListener
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource


/**
 * 可联动的二级缓存自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-linkable-cache.yml"],
    factory = YamlPropertySourceFactory::class
)
@ConditionalOnProperty(prefix = "kudos.ability.cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableCaching(proxyTargetClass = true)
open class LinkableCacheAutoConfiguration : IComponentInitializer {

    private val logger = LogFactory.getLog(this)

    @Primary
    @Bean(name = ["cacheManager"])
    open fun cacheManager(): MixCacheManager = MixCacheManager()

    @Bean
    @ConditionalOnMissingBean
    open fun mixCacheInitializing(): MixCacheInitializing = MixCacheInitializing()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheHandlerBeanPostProcessor(): CacheHandlerBeanPostProcessor = CacheHandlerBeanPostProcessor()

    @Bean
    @ConditionalOnMissingBean
    open fun batchCacheableAspect(): BatchCacheableAspect = BatchCacheableAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheNotifyListener(): CacheNotifyListener = CacheNotifyListener()

    override fun getComponentName() = "kudos-ability-cache-linkable"

}