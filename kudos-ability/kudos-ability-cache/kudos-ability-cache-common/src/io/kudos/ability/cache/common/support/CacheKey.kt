package io.kudos.ability.cache.common.support

import java.lang.annotation.Inherited

/**
 * 自定义缓存 key 注解。
 *
 * 与 Spring [org.springframework.cache.annotation.Cacheable.key] 语义一致——把方法参数 / 自身字段
 * 通过 SpEL 表达式拼成缓存 key。本模块的 `ContextKeyGenerator` 在解析 key 时会读取本注解，
 * 让"key 表达式"在类层级和方法层级独立声明，而不必塞进 `@Cacheable(key = "...")` 一处。
 *
 * @property value SpEL 表达式；空串表示使用 Spring 默认的简单 key 生成器
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

