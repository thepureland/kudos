package io.kudos.ability.cache.common.aop

import org.springframework.aot.hint.annotation.Reflective
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
@Reflective
annotation class DistributedCacheGuard 
