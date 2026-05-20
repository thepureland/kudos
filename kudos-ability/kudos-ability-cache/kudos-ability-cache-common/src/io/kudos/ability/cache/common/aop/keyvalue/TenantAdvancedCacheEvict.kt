package io.kudos.ability.cache.common.aop.keyvalue

import java.lang.annotation.Inherited

/**
 * 进阶租户缓存失效注解：与 [TenantAdvancedCacheable] 配套，
 * 由 [TenantAdvancedCacheEvictAspect] 处理，按 `cacheKey::tenantId` 拼装远端 key 后 evict。
 *
 * @property cacheKey 缓存键基名（必填）；运行时拼为 `cacheKey::tenantId`
 * @property dataKey 数据键（远端 hash 的 field 或子路径）；空表示整条 key
 * @property allEntries true 表示清空该 cacheKey 下的所有 entry
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
annotation class TenantAdvancedCacheEvict(
    val cacheKey: String,
    val dataKey: String = "",
    val allEntries: Boolean = false
)
