package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.batch.BatchCacheableAspect
import io.kudos.ability.cache.common.batch.DefaultKeysGenerator
import io.kudos.ability.cache.common.batch.IKeysGenerator
import io.kudos.ability.cache.common.core.CacheDataInitializer
import io.kudos.ability.cache.common.core.MixCacheInitializing
import io.kudos.ability.cache.common.core.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheNotifyListener
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
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

//    @Primary
//    @Bean("cacheManager")
//    open fun cacheManager(@Qualifier("mixCacheManager") mixCacheManager: MixCacheManager): CacheManager {
//        return TransactionAwareCacheManagerProxy(mixCacheManager)
//    }

    @Primary
    @Bean("cacheManager", "mixCacheManager")
    open fun mixCacheManager(): MixCacheManager = MixCacheManager()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheVersionConfig() = CacheVersionConfig()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheConfigProvider(cacheItemsProperties: CacheItemsProperties): ICacheConfigProvider =
        DefaultCacheConfigProvider(cacheItemsProperties)

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache")
    open fun cacheItemsProperties() = CacheItemsProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun mixCacheInitializing(): MixCacheInitializing = MixCacheInitializing()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheDataInitialize() = CacheDataInitializer()

    @Bean
    @ConditionalOnMissingBean
    open fun batchCacheableAspect(): BatchCacheableAspect = BatchCacheableAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheNotifyListener(): CacheNotifyListener = CacheNotifyListener()

    @Bean
    @ConditionalOnMissingBean
    open fun notityTool() = NotifyTool()

    @Bean
    @ConditionalOnMissingBean
    open fun simpleKeyGenerator(): KeyGenerator = SimpleKeyGenerator()

    @Bean("defaultKeysGenerator")
    @ConditionalOnMissingBean
    open fun keysGenerator(): IKeysGenerator = DefaultKeysGenerator()

    override fun getComponentName() = "kudos-ability-cache-linkable"

}