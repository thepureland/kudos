package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.LinkableCacheAutoConfiguration
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.local.caffeine.CaffeineHashCache
import io.kudos.ability.cache.local.caffeine.CaffeineKeyValueCacheManager
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
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
 * recognizes this annotation (equivalent to Spring Boot's default SPI `@AutoConfigureBefore`). Note that it maps
 * to Spring's `dependsOn`, which orders *instantiation* only — it does not order bean-definition registration,
 * so it cannot arbitrate `@ConditionalOnMissingBean` races. Hence the rule below.
 *
 * This class deliberately does **not** extend `BaseCacheConfiguration`. It used to, as did the Redis
 * counterpart, which meant the six shared beans (`cacheItemsProperties`, `cacheConfigProvider`, …) were
 * declared by three configuration classes at once and resolved by whichever definition Spring happened to
 * register first — an order that comes from classpath scanning and is therefore not deterministic. The
 * variants were not equivalent either: only [LinkableCacheAutoConfiguration]'s `cacheItemsProperties` carries
 * `@ConfigurationProperties`, so losing that race silently left `cache-items` unbound and no cache at all was
 * created. Shared beans now have exactly one owner: [LinkableCacheAutoConfiguration].
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
open class CaffeineCacheAutoConfiguration : IComponentInitializer {

    /**
     * Local K-V cache manager; bean name `localCacheManager` is injected by
     * [io.kudos.ability.cache.common.core.keyvalue.MixCacheManager] via `@Resource(name = "localCacheManager")`.
     *
     * The condition is keyed on that **name**, not on the type: a type-based `@ConditionalOnMissingBean` asks
     * "does any `IKeyValueCacheManager` exist?", so any unrelated implementation in the context (another local
     * cache, a test double, or a remote manager whose declared type is narrowed later) would suppress this bean.
     * `MixCacheManager` would then find no `localCacheManager`, and every `LOCAL_REMOTE` cache would quietly
     * degrade to `REMOTE` with only a single warn line to show for it.
     */
    @Bean(name = ["localCacheManager"])
    @ConditionalOnMissingBean(name = ["localCacheManager"])
    open fun caffeineCacheManager(): IKeyValueCacheManager<*> = CaffeineKeyValueCacheManager()

    /**
     * Local Hash cache properties.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.cache.local.caffeine.hash")
    open fun caffeineHashCacheProperties(): CaffeineHashCacheProperties = CaffeineHashCacheProperties()

    /**
     * Local Hash cache; bean name `caffeineIdEntitiesHashCache` pairs with the remote counterpart (e.g. `redisIdEntitiesHashCache`).
     *
     * The provider is looked up through an [ObjectProvider] and consulted lazily, per cache name: this
     * configuration deliberately loads *before* `LinkableCacheAutoConfiguration` (which owns
     * [ICacheConfigProvider]), so the bean does not exist yet at wiring time — only by the time the first hash
     * cache is actually created. A cache item with no `ttl` keeps the previous behaviour of never expiring on
     * its own.
     */
    @Bean("caffeineIdEntitiesHashCache")
    @ConditionalOnMissingBean(name = ["caffeineIdEntitiesHashCache"])
    open fun caffeineIdEntitiesHashCache(
        properties: CaffeineHashCacheProperties,
        cacheConfigProvider: ObjectProvider<ICacheConfigProvider>
    ): CaffeineHashCache = CaffeineHashCache(properties.maximumSize) { cacheName ->
        cacheConfigProvider.getIfAvailable()?.getHashCacheConfigs()?.get(cacheName)?.ttl
    }

    override fun getComponentName() = "kudos-ability-cache-local-caffeine"

}
