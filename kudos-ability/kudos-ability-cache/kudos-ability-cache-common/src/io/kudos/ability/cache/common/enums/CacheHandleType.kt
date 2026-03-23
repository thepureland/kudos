package io.kudos.ability.cache.common.enums

import io.kudos.base.enums.ienums.IDictEnum


/**
 * 缓存处理类型枚举
 *
 * @author admin
 * @date 16-4-11
 */
enum class CacheHandleType(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    OVERLOAD("overload", "重载指定key的緩存"),
    OVERLOAD_ALL("overloadAll", "重载所有緩存"),
    EVICT("evict", "剔除指定key的緩存"),
    EVICT_ALL("evictAll", "清除所有緩存"),
    GET_KEY("getKey", "檢查key是否存在"),
    GET_VALUE("getValue", "獲取key對應的值");

    companion object {
        fun get(code: String): CacheHandleType? {
            for (type in entries) {
                if (code == type.code) {
                    return type
                }
            }
            return null
        }
    }

}
