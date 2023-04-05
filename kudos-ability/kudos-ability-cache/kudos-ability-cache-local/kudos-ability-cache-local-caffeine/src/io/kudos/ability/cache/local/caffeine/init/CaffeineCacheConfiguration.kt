package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.local.caffeine.core.CaffeineCacheManager
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
@PropertySource(
    value = ["classpath:kudos-ability-cache-local-caffeine.yml"],
    factory = SoulPropertySourceFactory::class
)
@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)
@EnableConfigurationProperties(CacheProperties::class)
@Configuration
open class CaffeineCacheConfiguration : BaseCacheConfiguration() {

    @Bean(name = ["soulLocalCacheManager"])
    @ConditionalOnProperty(
        prefix = "kudos.ability.cache",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    @ConditionalOnMissingBean
    open fun caffeineCacheManager(): CaffeineCacheManager = CaffeineCacheManager()

}