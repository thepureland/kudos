package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.aot.hint.annotation.Reflective
import java.lang.annotation.Inherited

/**
 * Distributed cache guard annotation.
 * Marks methods or classes that require distributed cache protection, ensuring distributed consistency of cache operations.
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
@Reflective
annotation class DistributedCacheGuard 
