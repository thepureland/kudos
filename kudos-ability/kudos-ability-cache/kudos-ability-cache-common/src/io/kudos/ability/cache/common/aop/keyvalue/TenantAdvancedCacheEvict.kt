package io.kudos.ability.cache.common.aop.keyvalue

import java.lang.annotation.Inherited

/**
 * Advanced tenant cache eviction annotation: paired with [TenantAdvancedCacheable];
 * handled by [TenantAdvancedCacheEvictAspect], which assembles the remote key as `cacheKey::tenantId` before evicting.
 *
 * @property cacheKey base name of the cache key (required); assembled at runtime as `cacheKey::tenantId`
 * @property dataKey data key (the field or sub-path of the remote hash); empty means the entire key
 * @property allEntries true clears all entries under this cacheKey
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
annotation class TenantAdvancedCacheEvict(
    val cacheKey: String,
    val dataKey: String = "",
    val allEntries: Boolean = false
)
