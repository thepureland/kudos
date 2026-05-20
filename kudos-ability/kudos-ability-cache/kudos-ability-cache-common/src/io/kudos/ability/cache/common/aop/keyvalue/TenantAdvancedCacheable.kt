package io.kudos.ability.cache.common.aop.keyvalue

import java.lang.annotation.Inherited

/**
 * 进阶租户缓存注解：与 [TenantCacheable] 走 Spring 标准缓存抽象不同，
 * 本注解由专门的 `TenantAdvancedCacheableAspect` 处理，把缓存写到**远程缓存**
 * （通常是 Redis），且租户维度通过 `cacheKey::tenantId` 显式拼装。
 *
 * 适用场景：跨服务共享的缓存条目，需要绕过 Spring 本地 CacheManager 直接走远端。
 *
 * @property timeOut 缓存过期毫秒数；默认 1800000 ms (30 min)
 * @property cacheKey 缓存键基名；运行时会拼接为 `cacheKey::tenantId`
 * @property dataKey 数据键（远端 hash 的 field 或子路径）
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
annotation class TenantAdvancedCacheable(
    val timeOut: Long = 1800000,
    val cacheKey: String = "",
    val dataKey: String = ""
)
