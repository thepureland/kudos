package io.kudos.ability.cache.common.support

import java.io.Serial
import java.io.Serializable
import java.util.function.Supplier

/**
 * 缓存值包装器——表达"已查询但值为 null"与"未查询"的区别。
 *
 * Spring `Cache.ValueWrapper` 在 cache API 内部使用类似语义，但本类是 cache **业务侧**的
 * 表达：业务调用 `KeyValueCacheKit.get(...)` 时若希望区分"key 不在缓存"和"key 在但 value 是 null"，
 * 用本包装器比直接返回 `T?` 更明确。
 *
 * 实现采用 [Optional] 风格的 [orElse] / [orElseGet] / [orElseThrow] API；
 * 通过工厂 [of] / [empty] 构造，构造器私有。
 *
 * @author K
 * @since 1.0.0
 */
class CacheValueWrapper<T> private constructor(
    /**
     * 返回包装的实际值
     */
    val value: T?
) : Serializable {

    val isPresent: Boolean
        /**
         * 检查包装器是否包含值
         */
        get() = value != null

    /**
     * 获取包装的值，如果值不存在则返回指定的默认值
     */
    fun orElse(defaultValue: T?): T? {
        return value ?: defaultValue
    }

    /**
     * 获取包装的值，如果值不存在则从提供的 Supplier 获取默认值
     */
    fun orElseGet(supplier: Supplier<out T?>): T? {
        return value ?: supplier.get()
    }

    /**
     * 获取包装的值，如果值不存在则抛出指定的异常
     */
    fun <X : Throwable?> orElseThrow(exceptionSupplier: Supplier<out X?>): T {
        if (value != null) {
            return value
        } else {
            throw exceptionSupplier.get()
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID = 7369716185425581870L

        /**
         * 静态方法创建包装器，支持空值
         */
        fun <T> of(value: T?): CacheValueWrapper<T?> {
            return CacheValueWrapper(value)
        }

        /**
         * 静态方法创建空包装器
         */
        fun <T> empty(): CacheValueWrapper<T?> {
            return CacheValueWrapper(null)
        }
    }
}

