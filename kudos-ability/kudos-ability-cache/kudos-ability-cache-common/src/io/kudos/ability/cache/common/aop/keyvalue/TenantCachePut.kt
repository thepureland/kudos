package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.CachePut
import org.springframework.core.annotation.AliasFor

/**
 * Tenant-isolated version of [CachePut] — a composed annotation that presets `keyGenerator` to
 * `tenantCacheKeyGenerator` so that the put key carries the tenant dimension.
 *
 * The `@CachePut` meta-annotation is load-bearing; see
 * [io.kudos.ability.cache.common.aop.keyvalue.TenantCacheable] for why removing it breaks the annotation twice over.
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
@CachePut
annotation class TenantCachePut(
    @get:AliasFor(annotation = CachePut::class, attribute = "value")
    vararg val value: String = [],

    @get:AliasFor(annotation = CachePut::class, attribute = "cacheNames")
    val cacheNames: Array<String> = [], val suffix: String = "",

    @get:AliasFor(annotation = CachePut::class, attribute = "keyGenerator")
    val keyGenerator: String = "tenantCacheKeyGenerator",

    @get:AliasFor(annotation = CachePut::class, attribute = "cacheManager")
    val cacheManager: String = "",

    @get:AliasFor(annotation = CachePut::class, attribute = "cacheResolver")
    val cacheResolver: String = "",

    @get:AliasFor(annotation = CachePut::class, attribute = "condition")
    val condition: String = "",

    @get:AliasFor(annotation = CachePut::class, attribute = "unless")
    val unless: String = ""
)
