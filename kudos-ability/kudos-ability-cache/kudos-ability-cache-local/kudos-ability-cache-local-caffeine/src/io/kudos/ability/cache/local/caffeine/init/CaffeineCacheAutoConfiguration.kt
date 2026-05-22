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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * Caffeine 本地缓存装配入口。
 *
 * 注册两个 bean：
 *  - `localCacheManager` → [CaffeineKeyValueCacheManager]，给 `MixCacheManager` 用作"本地缓存层"
 *  - `caffeineIdEntitiesHashCache` → [CaffeineHashCache]，Hash 缓存的本地实现
 *
 * 通过 `@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)` 保证 `MixCacheManager`
 * 装配时已经能拿到 `localCacheManager`——kudos 自定义的 SPI 调度器 `ComponentInitializationDispatcher`
 * 识别这个注解（与 Spring Boot 默认 SPI 的 @AutoConfigureBefore 等价）。
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
open class CaffeineCacheAutoConfiguration : BaseCacheConfiguration(), IComponentInitializer {

    /** 本地 K-V 缓存管理器；bean 名 `localCacheManager` 由 [io.kudos.ability.cache.common.core.keyvalue.MixCacheManager] 注入。 */
    @Bean(name = ["localCacheManager"])
    @ConditionalOnMissingBean
    open fun caffeineCacheManager(): IKeyValueCacheManager<*> = CaffeineKeyValueCacheManager()

    /**
     * 本地 Hash 缓存配置。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.local.caffeine.hash")
    open fun caffeineHashCacheProperties(): CaffeineHashCacheProperties = CaffeineHashCacheProperties()

    /** 本地 Hash 缓存；bean 名 `caffeineIdEntitiesHashCache` 与远程版（如 `redisIdEntitiesHashCache`）配对存在。 */
    @Bean("caffeineIdEntitiesHashCache")
    @ConditionalOnMissingBean(name = ["caffeineIdEntitiesHashCache"])
    open fun caffeineIdEntitiesHashCache(properties: CaffeineHashCacheProperties): CaffeineHashCache =
        CaffeineHashCache(properties.maximumSize)

    override fun getComponentName() = "kudos-ability-cache-local-caffeine"

}
