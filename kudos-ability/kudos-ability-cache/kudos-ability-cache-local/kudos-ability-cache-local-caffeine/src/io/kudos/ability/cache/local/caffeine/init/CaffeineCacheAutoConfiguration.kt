package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.common.init.BaseCacheConfiguration
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.cache.common.support.ICacheManager
import org.soul.ability.cache.local.caffeine.CaffeineCacheManager
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import javax.annotation.PostConstruct


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
@ConditionalOnProperty(
    prefix = "kudos.ability.cache",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)
@EnableConfigurationProperties(CacheProperties::class)
//@Configuration
open class CaffeineCacheAutoConfiguration : BaseCacheConfiguration(), IComponentInitializer {

    @Bean(name = ["soulLocalCacheManager"])
    @ConditionalOnMissingBean
    open fun caffeineCacheManager(): ICacheManager<*> = CaffeineCacheManager()

    @PostConstruct
    override fun init() {
        LoggerFactory.getLogger(this).info("【kudos-ability-cache-local-caffeine】初始化完成.")
    }

}