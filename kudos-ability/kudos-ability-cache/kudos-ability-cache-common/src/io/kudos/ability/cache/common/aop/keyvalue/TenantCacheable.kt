package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * Tenant-isolated version of [Cacheable] — a composed annotation that presets `keyGenerator` to
 * `tenantCacheKeyGenerator` so that the cache key carries the tenant dimension.
 *
 * The `@Cacheable` meta-annotation is load-bearing and must not be removed: Spring Cache only builds cache
 * operations for methods meta-annotated with its own annotations, and `@AliasFor(annotation = Cacheable::class)`
 * is only legal on an annotation that actually is meta-annotated with [Cacheable]. Dropping it makes every
 * `@TenantCacheable` method silently uncached *and* makes reading the annotation throw
 * `AnnotationConfigurationException`. Guarded by `TenantCacheAnnotationsTest`.
 *
 * `value` and `cacheNames` map straight onto their [Cacheable] namesakes (rather than crosswise), following the
 * same convention as Spring's own composed annotations such as `@GetMapping`; because [Cacheable]'s own `value`
 * and `cacheNames` are mutual aliases, the two stay mirrors of each other here as well.
 *
 * @author K
 * @since 1.0.0
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Cacheable
annotation class TenantCacheable(

    @get:AliasFor(annotation = Cacheable::class, attribute = "value")
    vararg val value: String = [],

    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheNames")
    val cacheNames: Array<String> = [], val suffix: String = "",

    @get:AliasFor(annotation = Cacheable::class, attribute = "keyGenerator")
    val keyGenerator: String = "tenantCacheKeyGenerator",

    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheManager")
    val cacheManager: String = "",

    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheResolver")
    val cacheResolver: String = "",

    @get:AliasFor(annotation = Cacheable::class, attribute = "condition")
    val condition: String = "",

    @get:AliasFor(annotation = Cacheable::class, attribute = "unless")
    val unless: String = "",

    @get:AliasFor(annotation = Cacheable::class, attribute = "sync")
    val sync: Boolean = false

)
