package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.CacheEvict
import org.springframework.core.annotation.AliasFor

/**
 * Tenant-isolated version of [CacheEvict] — directly aliased to the Spring annotation via `@AliasFor`, simply presetting
 * the default value of `keyGenerator` to `tenantCacheKeyGenerator` so that the key carries the tenant dimension by default.
 *
 * In most cases business code only needs to write the annotation name plus cacheNames and does not need to specify
 * keyGenerator again.
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
//@CacheEvict(cacheNames = [])
annotation class TenantCacheEvict(
    @get:AliasFor(annotation = CacheEvict::class, attribute = "cacheNames")
    vararg val value: String = [],

    @get:AliasFor(annotation = CacheEvict::class, attribute = "value")
    val cacheNames: Array<String> = [], val suffix: String = "",

    @get:AliasFor(annotation = CacheEvict::class, attribute = "keyGenerator")
    val keyGenerator: String = "tenantCacheKeyGenerator",

    @get:AliasFor(annotation = CacheEvict::class, attribute = "cacheManager")
    val cacheManager: String = "",

    @get:AliasFor(annotation = CacheEvict::class, attribute = "cacheResolver")
    val cacheResolver: String = "",

    @get:AliasFor(annotation = CacheEvict::class, attribute = "condition")
    val condition: String = "",

    @get:AliasFor(annotation = CacheEvict::class, attribute = "allEntries")
    val allEntries: Boolean = false,

    @get:AliasFor(annotation = CacheEvict::class, attribute = "beforeInvocation")
    val beforeInvocation: Boolean = false
)
