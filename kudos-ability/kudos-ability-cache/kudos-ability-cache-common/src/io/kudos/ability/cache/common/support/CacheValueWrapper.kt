package io.kudos.ability.cache.common.support

import java.io.Serial
import java.io.Serializable
import java.util.function.Supplier

/**
 * 缓存值包装器
 * @param <T>
</T> */
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
        return if (value != null) value else defaultValue
    }

    /**
     * 获取包装的值，如果值不存在则从提供的 Supplier 获取默认值
     */
    fun orElseGet(supplier: Supplier<out T?>): T? {
        return if (value != null) value else supplier.get()
    }

    /**
     * 获取包装的值，如果值不存在则抛出指定的异常
     */
    @Throws(X::class)
    fun <X : Throwable?> orElseThrow(exceptionSupplier: Supplier<out X?>): T? {
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
            return CacheValueWrapper<T?>(value)
        }

        /**
         * 静态方法创建空包装器
         */
        fun <T> empty(): CacheValueWrapper<T?> {
            return CacheValueWrapper<T?>(null)
        }
    }
}

