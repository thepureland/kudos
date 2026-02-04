package io.kudos.ability.cache.common.aop.keyvalue


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TenantCaching(val evicts: Array<TenantCacheEvict>)
