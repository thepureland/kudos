package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.core.LinkableCacheManager
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.cache.common.CacheHandlerBeanPostProcessor
import org.soul.ability.cache.common.MixCacheInitializing
import org.soul.ability.cache.common.batch.BatchCacheableAspect
import org.soul.ability.cache.common.notify.CacheNotifyListener
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import javax.annotation.PostConstruct


/**
 * 可联动的二级缓存自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@ComponentScan(
    basePackages = [
        "io.kudos.ability.cache.common",
        "io.kudos.ability.cache.local",
        "io.kudos.ability.cache.remote",
    ],
)
@PropertySource(
    value = ["classpath:kudos-ability-linkable-cache.yml"],
    factory = SoulPropertySourceFactory::class
)
@ConditionalOnProperty(prefix = "kudos.ability.cache", name = ["enabled"], havingValue = "true", matchIfMissing = false)
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableCaching(proxyTargetClass = true)
open class LinkableCacheAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @Primary
    @Bean(name = ["cacheManager"])
    open fun cacheManager(): CacheManager = LinkableCacheManager()

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

    @PostConstruct
    override fun init() {
        logger.info("【kudos-ability-cache-linkable】初始化完成.")
    }

}