package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimaryAspect
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondaryAspect
import io.kudos.ability.cache.common.aop.keyvalue.DistributedCacheGuardAspect
import io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheEvictAspect
import io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheableAspect
import io.kudos.ability.cache.common.aop.keyvalue.TenantCachingAspect
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
import io.kudos.ability.cache.common.support.ContextKeyGenerator
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
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

    /**
     * Key generator behind the `@Tenant*` annotation family. The bean **name** is the contract here:
     * [io.kudos.ability.cache.common.aop.keyvalue.TenantCacheable] and friends default their
     * `keyGenerator` attribute to the literal `"tenantCacheKeyGenerator"`, and Spring Cache resolves that
     * attribute by name. Without this registration every `@TenantCacheable` method fails at runtime with
     * `NoSuchBeanDefinitionException`, so the condition is keyed on the name rather than the type.
     */
    @Bean("tenantCacheKeyGenerator")
    @ConditionalOnMissingBean(name = ["tenantCacheKeyGenerator"])
    open fun tenantCacheKeyGenerator(): TenantCacheKeyGenerator = TenantCacheKeyGenerator()

    /**
     * Conditioned on the bean *name*: [TenantCacheKeyGenerator] is itself a [KeyGenerator], so a type-based
     * `@ConditionalOnMissingBean` would let whichever generator happens to be registered first suppress the
     * other one — an order-dependent coin flip between two beans that are both required.
     */
    @Bean("simpleKeyGenerator")
    @ConditionalOnMissingBean(name = ["simpleKeyGenerator"])
    open fun simpleKeyGenerator(): KeyGenerator = SimpleKeyGenerator()

    /**
     * Key generator behind `@CacheKey`. Resolved by name from the `keyGenerator` attribute, so — like the two
     * generators above — its condition is keyed on the bean name rather than on [KeyGenerator].
     *
     * Previously only [BaseCacheConfiguration] declared it, which meant it reached the context solely because
     * the Caffeine/Redis auto-configurations happened to inherit that class; it is registered here now that
     * shared beans have a single owner.
     */
    @Bean("contextKeyGenerator")
    @ConditionalOnMissingBean(name = ["contextKeyGenerator"])
    open fun contextKeyGenerator(): ContextKeyGenerator = ContextKeyGenerator()

    /**
     * `@TenantCaching` aspect. The class carries `@Component`, but kudos assembles components by importing
     * [io.kudos.context.init.IComponentInitializer] configuration classes rather than by scanning
     * `io.kudos.ability.**`, so that annotation never took effect and the whole annotation was a silent no-op.
     * Same oversight (and same fix) as [distributedCacheGuardAspect] above.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun tenantCachingAspect(): TenantCachingAspect = TenantCachingAspect()

    /** `@TenantAdvancedCacheable` aspect; see [tenantCachingAspect] for why explicit registration is required. */
    @Bean
    @ConditionalOnMissingBean
    open fun tenantAdvancedCacheableAspect(): TenantAdvancedCacheableAspect = TenantAdvancedCacheableAspect()

    /** `@TenantAdvancedCacheEvict` aspect; see [tenantCachingAspect] for why explicit registration is required. */
    @Bean
    @ConditionalOnMissingBean
    open fun tenantAdvancedCacheEvictAspect(): TenantAdvancedCacheEvictAspect = TenantAdvancedCacheEvictAspect()

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