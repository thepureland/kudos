package io.kudos.ability.cache.common.enums

import io.kudos.base.enums.ienums.IDictEnum


/**
 * 缓存处理类型枚举。
 *
 * 用于 cache 后台管理 / 监控页面下发"对某缓存做某操作"的指令；本模块自身的 AOP
 * 流程并不直接消费这个枚举（它们走 `@TenantCacheable` / `@TenantCacheEvict` 等注解）。
 *
 * @author K
 * @since 1.0.0
 */
enum class CacheHandleType(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    /** 重载指定 key 的缓存。 */
    OVERLOAD("overload", "重载指定key的緩存"),

    /** 重载整个缓存的所有 key。 */
    OVERLOAD_ALL("overloadAll", "重载所有緩存"),

    /** 剔除指定 key 的缓存项。 */
    EVICT("evict", "剔除指定key的緩存"),

    /** 清空整个缓存。 */
    EVICT_ALL("evictAll", "清除所有緩存"),

    /** 检查 key 是否存在（不返回 value）。 */
    GET_KEY("getKey", "檢查key是否存在"),

    /** 获取 key 对应的值。 */
    GET_VALUE("getValue", "獲取key對應的值");

    companion object {
        /** 按 [code] 字面值查询枚举；未匹配返回 null。 */
        fun get(code: String): CacheHandleType? = entries.firstOrNull { it.code == code }
    }

}
