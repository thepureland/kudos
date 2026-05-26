package io.kudos.ability.cache.common.aop.keyvalue

import java.lang.annotation.Inherited

/**
 * Advanced tenant cache annotation: unlike [TenantCacheable] which goes through Spring's standard cache abstraction,
 * this annotation is handled by a dedicated `TenantAdvancedCacheableAspect` that writes the cache directly to the
 * **remote cache** (typically Redis), with the tenant dimension explicitly assembled via `cacheKey::tenantId`.
 *
 * Use case: cache entries shared across services that need to bypass Spring's local CacheManager and go directly to the
 * remote cache.
 *
 * @property timeOut cache expiration in milliseconds; default 1800000 ms (30 min)
 * @property cacheKey base name of the cache key; assembled at runtime as `cacheKey::tenantId`
 * @property dataKey data key (the field or sub-path of the remote hash)
 * @author K
 * @since 1.0.0
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
annotation class TenantAdvancedCacheable(
    val timeOut: Long = 1800000,
    val cacheKey: String = "",
    val dataKey: String = ""
)
