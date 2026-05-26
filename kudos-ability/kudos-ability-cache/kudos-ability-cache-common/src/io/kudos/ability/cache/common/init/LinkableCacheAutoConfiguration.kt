package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimaryAspect
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondaryAspect
import io.kudos.ability.cache.common.aop.keyvalue.DistributedCacheGuardAspect
import io.kudos.ability.cache.common.batch.hash.DefaultHashBatchKeysGenerator
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimaryAspect
import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheableAspect
import io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator
import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.ability.cache.common.core.CacheDataInitializer
import io.kudos.ability.cache.common.core.MixCacheInitializing
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheNotifyListener
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import io.kudos.ability.cache.common.support.ICacheConfigProvider
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
 * Auto-configuration for the linkable two-level cache.
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

    @Bean("mixHashCacheManager")
    @ConditionalOnMissingBean
    open fun mixHashCacheManager(): MixHashCacheManager = MixHashCacheManager()

    @Bean
    @ConditionalOnMissingBean
    open fun mixCacheInitializing(): MixCacheInitializing = MixCacheInitializing()

    /**
     * See `cacheDataInitialize` in the [companion object]: an @Bean factory that returns a `BeanPostProcessor`
     * must be static; otherwise the configuration class is forced to instantiate early and skip processing by
     * other BPPs.
     */

    @Bean
    @ConditionalOnMissingBean
    open fun batchCacheableAspect(): BatchCacheableAspect = BatchCacheableAspect()

    /**
     * `@DistributedCacheGuard` aspect. The class was previously annotated with `@Aspect` but never registered
     * as an @Bean, so the annotation was a no-op at runtime; this registration fixes that oversight.
     * Guarded by [ConditionalOnMissingBean] so business-side overrides do not conflict.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun distributedCacheGuardAspect(): DistributedCacheGuardAspect = DistributedCacheGuardAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun hashCacheableByPrimaryAspect(): HashCacheableByPrimaryAspect = HashCacheableByPrimaryAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun hashCacheableBySecondaryAspect(): HashCacheableBySecondaryAspect = HashCacheableBySecondaryAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun hashBatchCacheableByPrimaryAspect(): HashBatchCacheableByPrimaryAspect = HashBatchCacheableByPrimaryAspect()

    @Bean("defaultHashBatchKeysGenerator")
    @ConditionalOnMissingBean(name = ["defaultHashBatchKeysGenerator"])
    open fun defaultHashBatchKeysGenerator(): DefaultHashBatchKeysGenerator = DefaultHashBatchKeysGenerator()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheNotifyListener(): CacheNotifyListener = CacheNotifyListener()

    @Bean
    @ConditionalOnMissingBean
    open fun simpleKeyGenerator(): KeyGenerator = SimpleKeyGenerator()

    @Bean("defaultKeysGenerator")
    @ConditionalOnMissingBean
    open fun keysGenerator(): IKeysGenerator = DefaultKeysGenerator()

    override fun getComponentName() = "kudos-ability-cache-linkable"

    companion object {
        /**
         * `CacheDataInitializer` implements `BeanPostProcessor`. An @Bean factory method returning a BPP
         * must be declared static (in Kotlin, via `companion object` + `@JvmStatic`); otherwise Spring is
         * forced to instantiate `LinkableCacheAutoConfiguration` early, making the configuration class itself
         * skip BPP processing and dragging along other AutoConfigurations that depend on it (the logs will
         * show ContextAutoConfiguration / various CacheAutoConfiguration instances affected).
         */
        @Bean
        @ConditionalOnMissingBean
        @JvmStatic
        fun cacheDataInitialize(): CacheDataInitializer = CacheDataInitializer()
    }

}