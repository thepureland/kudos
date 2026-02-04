package io.kudos.ability.cache.common.aop.keyvalue

import java.lang.annotation.Inherited

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
