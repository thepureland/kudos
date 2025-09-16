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
    override val trans: String
) : IDictEnum {

    OVERLOAD("overload", "重载"),
    OVERLOAD_ALL("overloadAll", "重载所有"),
    EVICT("evict", "剔除"),
    EVICT_ALL("evictAll", "清除所有"),
    GET_KEY("getKey", "重载"),
    GET_VALUE("getValue", "重载所有");

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
