package io.kudos.ability.cache.common.aop

import org.springframework.cache.annotation.CachePut
import org.springframework.core.annotation.AliasFor

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CachePut(cacheNames = [])
annotation class TenantCachePut(
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "cacheNames"
    ) vararg val value: String = [],
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "value"
    ) val cacheNames: Array<String> = [],
    val suffix: String = "",
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "keyGenerator"
    ) val keyGenerator: String = "tenantCacheKeyGenerator",
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "cacheManager"
    ) val cacheManager: String = "",
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "cacheResolver"
    ) val cacheResolver: String = "",
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "condition"
    ) val condition: String = "",
    @get:AliasFor(
        annotation = CachePut::class,
        attribute = "unless"
    ) val unless: String = ""
)
