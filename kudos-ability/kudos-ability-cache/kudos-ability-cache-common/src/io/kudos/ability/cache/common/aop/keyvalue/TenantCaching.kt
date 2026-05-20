package io.kudos.ability.cache.common.aop.keyvalue


/**
 * 容器注解：把多个 [TenantCacheEvict] 一起标到同一方法上。
 *
 * Kotlin 注解默认不允许在同一目标重复出现，所以需要外层容器（与 Spring 的 [@Caching] 同思路）。
 * AOP 切面会展开 `evicts` 数组依次执行 evict。
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TenantCaching(val evicts: Array<TenantCacheEvict>)
