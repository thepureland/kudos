package io.kudos.ability.cache.common.support

import java.lang.annotation.Inherited

/**
 * Custom cache key annotation.
 *
 * Semantically equivalent to Spring's [org.springframework.cache.annotation.Cacheable.key] — combines method
 * arguments / instance fields into a cache key via a SpEL expression. This module's `ContextKeyGenerator`
 * reads this annotation when resolving the key, allowing the "key expression" to be declared independently
 * at the class and method levels rather than being crammed into a single `@Cacheable(key = "...")`.
 *
 * @property value SpEL expression; an empty string falls back to Spring's default simple key generator
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
annotation class CacheKey(
    val value: String = ""
)

