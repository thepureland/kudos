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
 * 基础缓存配置类
 * 
 * Spring Boot自动配置类，用于配置缓存相关的Bean。
 * 所有Bean都使用@ConditionalOnMissingBean，支持用户自定义覆盖。
 * 
 * 配置的Bean：
 * 1. cacheVersionConfig：缓存版本配置，用于控制缓存版本
 * 2. cacheItemsProperties：缓存项属性配置，用于配置各个缓存的属性
 * 3. cacheConfigProvider：缓存配置提供者，用于提供缓存配置信息
 * 4. contextKeyGenerator：上下文键生成器，用于生成包含上下文信息的缓存键
 * 5. simpleKeyGenerator：简单键生成器，Spring Cache默认的键生成器
 * 6. defaultKeysGenerator：默认键生成器，用于批量缓存的键生成
 * 
 * 条件配置：
 * - 所有Bean都使用@ConditionalOnMissingBean注解
 * - 如果用户已经定义了同类型的Bean，则不会创建默认Bean
 * - 支持用户完全自定义缓存配置
 * 
 * 使用场景：
 * - Spring Boot应用的缓存自动配置
 * - 多模块项目的缓存配置基础
 * - 需要自定义缓存配置时的基类
 * 
 * 注意事项：
 * - 这是一个open类，可以被继承和覆盖
 * - 所有Bean都是可选的，可以根据需要选择性使用
 * - 建议在子类中覆盖需要自定义的Bean方法
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