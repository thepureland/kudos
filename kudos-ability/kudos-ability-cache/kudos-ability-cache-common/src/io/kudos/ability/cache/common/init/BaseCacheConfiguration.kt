package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator
import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.ContextKeyGenerator
import io.kudos.ability.cache.common.support.DefaultCacheConfigProvider
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean


/**
 * Optional mixin that registers the shared cache beans.
 *
 * **Not used by kudos itself.** [LinkableCacheAutoConfiguration] is the single owner of these beans; the
 * Caffeine and Redis auto-configurations used to extend this class, which meant the same six beans were
 * declared three times over and the winner was decided by classpath-scan order. Since the variants were not
 * equivalent (this one's `cacheItemsProperties` lacked `@ConfigurationProperties`), losing that race left
 * `cache-items` unbound and no cache was created at all. That inheritance has been removed.
 *
 * The class is kept for applications that assemble the cache stack by hand — e.g. a service that pulls in
 * `cache-common` without the kudos auto-configuration chain. It is now attribute-for-attribute equivalent to
 * the declarations in [LinkableCacheAutoConfiguration], so extending it is safe even when that configuration
 * is also active: every bean is conditioned on its **name**, so whichever is registered first wins and the
 * other backs off, and both produce the same result either way.
 *
 * Beans registered:
 * 1. cacheVersionConfig: cache version configuration for cache versioning.
 * 2. cacheItemsProperties: cache-items property configuration for per-cache attributes.
 * 3. cacheConfigProvider: cache configuration provider that supplies cache configs.
 * 4. contextKeyGenerator: context key generator that builds cache keys including context information.
 * 5. simpleKeyGenerator: simple key generator (the Spring Cache default).
 * 6. defaultKeysGenerator: default keys generator for batch cache key generation.
 *
 * To customize, declare your own bean under the same name, or subclass and override the factory method.
 *
 * @since 1.0.0
 */
open class BaseCacheConfiguration {

    @Bean("cacheVersionConfig")
    @ConditionalOnMissingBean(name = ["cacheVersionConfig"])
    open fun cacheVersionConfig(): CacheVersionConfig = CacheVersionConfig()

    /**
     * Carries `@ConfigurationProperties` so that `kudos.ability.cache.cache-items[]` /
     * `cache-item-configs[]` are actually bound. Without it the container gets an empty
     * [CacheItemsProperties] and [DefaultCacheConfigProvider] reports `size=0` — no cache is ever created,
     * and the only symptom is one info line at startup.
     */
    @Bean("cacheItemsProperties")
    @ConditionalOnMissingBean(name = ["cacheItemsProperties"])
    @ConfigurationProperties(prefix = "kudos.ability.cache")
    open fun cacheItemsProperties(): CacheItemsProperties = CacheItemsProperties()

    @Bean("cacheConfigProvider")
    @ConditionalOnMissingBean(name = ["cacheConfigProvider"])
    open fun cacheConfigProvider(itemsProperties: CacheItemsProperties): ICacheConfigProvider =
        DefaultCacheConfigProvider(itemsProperties)

    @Bean("contextKeyGenerator")
    @ConditionalOnMissingBean(name = ["contextKeyGenerator"])
    open fun myKeyGenerator(): ContextKeyGenerator = ContextKeyGenerator()

    /**
     * Conditioned on the bean name rather than on [KeyGenerator]: several generators in this module implement
     * that interface, so a type-based condition would let one of them suppress the others arbitrarily.
     */
    @Bean("simpleKeyGenerator")
    @ConditionalOnMissingBean(name = ["simpleKeyGenerator"])
    open fun simpleKeyGenerator(): KeyGenerator = SimpleKeyGenerator()

    @Bean("defaultKeysGenerator")
    @ConditionalOnMissingBean(name = ["defaultKeysGenerator"])
    open fun keysGenerator(): IKeysGenerator = DefaultKeysGenerator()

}