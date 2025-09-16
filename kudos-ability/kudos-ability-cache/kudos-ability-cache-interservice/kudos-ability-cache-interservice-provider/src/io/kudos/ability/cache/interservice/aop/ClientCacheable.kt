package io.kudos.ability.cache.interservice.aop

import java.lang.annotation.Inherited

/**
 * 服务端controller注解
 * 用于告知客户端此次请求是否需要缓存
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