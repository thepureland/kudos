package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator
import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
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
 * Base cache configuration.
 *
 * Spring Boot auto-configuration class that registers cache-related beans.
 * All beans use @ConditionalOnMissingBean so user-defined beans can override them.
 *
 * Beans registered:
 * 1. cacheVersionConfig: cache version configuration for cache versioning.
 * 2. cacheItemsProperties: cache-items property configuration for per-cache attributes.
 * 3. cacheConfigProvider: cache configuration provider that supplies cache configs.
 * 4. contextKeyGenerator: context key generator that builds cache keys including context information.
 * 5. simpleKeyGenerator: simple key generator (the Spring Cache default).
 * 6. defaultKeysGenerator: default keys generator for batch cache key generation.
 *
 * Conditional configuration:
 * - Every bean is annotated with @ConditionalOnMissingBean.
 * - If the user has defined a bean of the same type, the default bean is not created.
 * - Supports fully customized cache configuration.
 *
 * Use cases:
 * - Cache auto-configuration for Spring Boot applications.
 * - Base cache configuration for multi-module projects.
 * - Base class for applications that need to customize cache configuration.
 *
 * Caveats:
 * - This is an open class that can be subclassed and overridden.
 * - All beans are optional; choose them as needed.
 * - Override the desired bean methods in a subclass.
 *
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