package io.kudos.ability.cache.common.aop

import org.springframework.aot.hint.annotation.Reflective
import java.lang.annotation.Inherited

/**
 * 分布式缓存保护注解
 * 用于标记需要分布式缓存保护的方法或类，确保缓存操作的分布式一致性
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
