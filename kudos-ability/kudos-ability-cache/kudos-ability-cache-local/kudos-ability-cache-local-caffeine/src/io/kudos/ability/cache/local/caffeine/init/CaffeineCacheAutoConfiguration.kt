package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.local.caffeine.CaffeineHashCache
import io.kudos.ability.cache.local.caffeine.CaffeineKeyValueCacheManager
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.cache.autoconfigure.CacheProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role


/**
 * Caffeine local cache wiring entry point.
 *
 * Registers two beans:
 *  - `localCacheManager` → [CaffeineKeyValueCacheManager], used by `MixCacheManager` as the "local cache layer"
 *  - `caffeineIdEntitiesHashCache` → [CaffeineHashCache], the local implementation of Hash cache
 *
 * `@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)` ensures `localCacheManager` is available
 * by the time `MixCacheManager` is wired — kudos's custom SPI dispatcher `ComponentInitializationDispatcher`
 * recognizes this annotation (equivalent to Spring Boot's default SPI `@AutoConfigureBefore`).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-cache-local-caffeine.yml"],
    factory = YamlPropertySourceFactory::class
)
@ConditionalOnProperty(
    prefix = "kudos.ability.cache",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableConfigurationProperties(CacheProperties::class)
// See ContextAutoConfiguration: IComponentInitializer configuration classes must be instantiated before
// business BPPs; ROLE_INFRASTRUCTURE avoids false positives from Spring's BeanPostProcessorChecker.
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
open class CaffeineCacheAutoConfiguration : BaseCacheConfiguration(), IComponentInitializer {

    /** Local K-V cache manager; bean name `localCacheManager` is injected by [io.kudos.ability.cache.common.core.keyvalue.MixCacheManager]. */
    @Bean(name = ["localCacheManager"])
    @ConditionalOnMissingBean
    open fun caffeineCacheManager(): IKeyValueCacheManager<*> = CaffeineKeyValueCacheManager()

    /**
     * Local Hash cache properties.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.local.caffeine.hash")
    open fun caffeineHashCacheProperties(): CaffeineHashCacheProperties = CaffeineHashCacheProperties()

    /** Local Hash cache; bean name `caffeineIdEntitiesHashCache` pairs with the remote counterpart (e.g. `redisIdEntitiesHashCache`). */
    @Bean("caffeineIdEntitiesHashCache")
    @ConditionalOnMissingBean(name = ["caffeineIdEntitiesHashCache"])
    open fun caffeineIdEntitiesHashCache(properties: CaffeineHashCacheProperties): CaffeineHashCache =
        CaffeineHashCache(properties.maximumSize)

    override fun getComponentName() = "kudos-ability-cache-local-caffeine"

}
