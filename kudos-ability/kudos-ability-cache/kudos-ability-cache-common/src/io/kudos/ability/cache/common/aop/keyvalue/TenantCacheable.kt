package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * 租户隔离版本的 [Cacheable]——`@AliasFor` 直通 Spring 注解，仅把
 * `keyGenerator` 默认值预置为 `tenantCacheKeyGenerator`，让缓存 key 自带租户维度。
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
//@Cacheable
annotation class TenantCacheable(

    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheNames")
    vararg val value: String = [],

    @get:AliasFor(annotation = Cacheable::class, attribute = "value")
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
