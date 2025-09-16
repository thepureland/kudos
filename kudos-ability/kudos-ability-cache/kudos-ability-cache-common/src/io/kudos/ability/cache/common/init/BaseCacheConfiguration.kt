package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.batch.DefaultKeysGenerator
import io.kudos.ability.cache.common.batch.IKeysGenerator
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.ContextKeyGenerator
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean


/**
 * 基础缓存配置类
 *
 * @author K
 * @since 1.0.0
 */
open class BaseCacheConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun cacheVersionConfig(): CacheVersionConfig = CacheVersionConfig()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheItemsProperties(): CacheItemsProperties = CacheItemsProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun cacheConfigProvider(itemsProperties: CacheItemsProperties): ICacheConfigProvider =
        DefaultCacheConfigProvider(itemsProperties)

    @Bean("contextKeyGenerator")
    @ConditionalOnMissingBean
    open fun myKeyGenerator(): ContextKeyGenerator = ContextKeyGenerator()

    @Bean
    @ConditionalOnMissingBean
    open fun simpleKeyGenerator(): KeyGenerator = SimpleKeyGenerator()

    @Bean("defaultKeysGenerator")
    @ConditionalOnMissingBean
    open fun keysGenerator(): IKeysGenerator = DefaultKeysGenerator()

}