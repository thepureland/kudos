package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

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
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "cacheNames"
    ) vararg val value: String = [],
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "value"
    ) val cacheNames: Array<String> = [],
    val suffix: String = "",
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "keyGenerator"
    ) val keyGenerator: String = "tenantCacheKeyGenerator",
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "cacheManager"
    ) val cacheManager: String = "",
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "cacheResolver"
    ) val cacheResolver: String = "",
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "condition"
    ) val condition: String = "",
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "unless"
    ) val unless: String = "",
    @get:AliasFor(
        annotation = Cacheable::class,
        attribute = "sync"
    ) val sync: Boolean = false
)
