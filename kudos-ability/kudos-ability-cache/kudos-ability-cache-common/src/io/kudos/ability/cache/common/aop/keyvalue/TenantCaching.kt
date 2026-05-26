package io.kudos.ability.cache.common.aop.keyvalue


/**
 * Container annotation: applies multiple [TenantCacheEvict] annotations together on the same method.
 *
 * Kotlin annotations do not allow repeated occurrences on the same target by default, so an outer container is needed
 * (the same idea as Spring's [@Caching]). The AOP aspect unwraps the `evicts` array and performs each evict in turn.
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TenantCaching(val evicts: Array<TenantCacheEvict>)
