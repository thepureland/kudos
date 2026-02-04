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
annotation class TenantAdvancedCacheable(
    val timeOut: Long = 1800000,
    val cacheKey: String = "",
    val dataKey: String = ""
)
