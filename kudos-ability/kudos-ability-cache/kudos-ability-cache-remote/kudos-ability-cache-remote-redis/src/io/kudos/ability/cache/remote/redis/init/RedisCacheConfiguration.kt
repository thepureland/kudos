package io.kudos.ability.cache.remote.redis.init

import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * redis缓存自动配置类
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
open class RedisCacheConfiguration {




}