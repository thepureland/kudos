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
     * 见 [companion object] 中的 `cacheDataInitialize`：返回 `BeanPostProcessor` 的 @Bean 必须 static，
     * 否则强制配置类自身提前实例化，跳过其它 BPP 的处理。
     */

    @Bean
    @ConditionalOnMissingBean
    open fun batchCacheableAspect(): BatchCacheableAspect = BatchCacheableAspect()

    /**
     * `@DistributedCacheGuard` 切面。原本类只 `@Aspect` 标注却没有 @Bean 注册，
     * 导致注解在运行时是 no-op；本注册修复该疏漏。受 [ConditionalOnMissingBean] 保护，
     * 业务侧覆盖时不冲突。
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
         * `CacheDataInitializer` 实现了 `BeanPostProcessor`。返回 BPP 的 @Bean 工厂方法
         * 必须声明为 static（Kotlin 用 `companion object` + `@JvmStatic`），否则 Spring 强制
         * 提前实例化 `LinkableCacheAutoConfiguration`，让该配置类自身跳过 BPP 处理，
         * 同时拖累其它依赖它的 AutoConfiguration（日志中能看到 ContextAutoConfiguration / 各 CacheAutoConfiguration 被牵连）。
         */
        @Bean
        @ConditionalOnMissingBean
        @JvmStatic
        fun cacheDataInitialize(): CacheDataInitializer = CacheDataInitializer()
    }

}