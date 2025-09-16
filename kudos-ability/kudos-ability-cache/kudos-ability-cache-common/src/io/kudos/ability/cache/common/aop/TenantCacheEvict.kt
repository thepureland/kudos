package io.kudos.ability.cache.common.aop

import org.springframework.cache.annotation.CacheEvict
import org.springframework.core.annotation.AliasFor

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CacheEvict(cacheNames = [])
annotation class TenantCacheEvict(
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "cacheNames"
    ) vararg val value: String = [],
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "value"
    ) val cacheNames: Array<String> = [],
    val suffix: String = "",
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "keyGenerator"
    ) val keyGenerator: String = "tenantCacheKeyGenerator",
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "cacheManager"
    ) val cacheManager: String = "",
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "cacheResolver"
    ) val cacheResolver: String = "",
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "condition"
    ) val condition: String = "",
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "allEntries"
    ) val allEntries: Boolean = false,
    @get:AliasFor(
        annotation = CacheEvict::class,
        attribute = "beforeInvocation"
    ) val beforeInvocation: Boolean = false
)
