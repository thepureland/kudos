package io.kudos.ability.cache.interservice.aop

import java.lang.annotation.Inherited

/**
 * Server-side controller annotation.
 * Tells the client whether this request needs to be cached.
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
annotation class ClientCacheable 