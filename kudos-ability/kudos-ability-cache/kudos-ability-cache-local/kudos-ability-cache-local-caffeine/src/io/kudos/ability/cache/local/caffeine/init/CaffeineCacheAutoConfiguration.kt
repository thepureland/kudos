package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.local.caffeine.CaffeineKeyValueCacheManager
import io.kudos.ability.cache.local.caffeine.CaffeineHashCache
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.cache.autoconfigure.CacheProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * Caffeine自动配置类
 *
 * @author K
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

    @Bean(name = ["localCacheManager"])
    @ConditionalOnMissingBean
    open fun caffeineCacheManager(): IKeyValueCacheManager<*> = CaffeineKeyValueCacheManager()

    @Bean("caffeineIdEntitiesHashCache")
    @ConditionalOnMissingBean(name = ["caffeineIdEntitiesHashCache"])
    open fun caffeineIdEntitiesHashCache(): CaffeineHashCache = CaffeineHashCache()

    override fun getComponentName() = "kudos-ability-cache-local-caffeine"

}