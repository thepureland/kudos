package io.kudos.ability.cache.common.support

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
annotation class CacheKey(
    /**
     * 缓存key
     *
     * @see {@link Cacheable.key
     */
    val value: String = ""
)

