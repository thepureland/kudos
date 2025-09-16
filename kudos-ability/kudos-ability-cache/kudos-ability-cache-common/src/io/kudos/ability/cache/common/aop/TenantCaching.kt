package io.kudos.ability.cache.common.aop


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TenantCaching(val evicts: Array<TenantCacheEvict>)
