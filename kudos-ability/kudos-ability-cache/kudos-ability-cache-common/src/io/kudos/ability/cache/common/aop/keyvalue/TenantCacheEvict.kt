package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.CacheEvict
import org.springframework.core.annotation.AliasFor

/**
 * 租户隔离版本的 [CacheEvict]——通过 `@AliasFor` 直通 Spring 注解，仅把
 * `keyGenerator` 的默认值预置为 `tenantCacheKeyGenerator`，让 key 自带租户维度。
 *
 * 业务侧大多数情况下只需写注解名 + cacheNames，无需重复指定 keyGenerator。
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
