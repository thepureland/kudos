package io.kudos.ability.cache.common.support

import java.io.Serial
import java.io.Serializable
import java.util.function.Supplier

/**
 * Cache value wrapper — distinguishes "queried, value is null" from "not queried".
 *
 * Spring's `Cache.ValueWrapper` uses similar semantics inside the cache API, but this class is the
 * cache **business-side** representation: when callers of `KeyValueCacheKit.get(...)` need to distinguish
 * "key is absent from the cache" from "key is present but value is null", this wrapper is clearer than returning `T?`.
 *
 * Provides an [Optional]-style [orElse] / [orElseGet] / [orElseThrow] API;
 * constructed via the [of] / [empty] factories, with a private constructor.
 *
 * @author K
 * @since 1.0.0
 */
class CacheValueWrapper<T> private constructor(
    /**
     * The wrapped actual value.
     */
    val value: T?
) : Serializable {

    val isPresent: Boolean
        /**
         * Checks whether the wrapper contains a value.
         */
        get() = value != null

    /**
     * Returns the wrapped value, or the given default value if absent.
     */
    fun orElse(defaultValue: T?): T? = value ?: defaultValue

    /**
     * Returns the wrapped value, or the value supplied by the given Supplier if absent.
     */
    fun orElseGet(supplier: Supplier<out T?>): T? = value ?: supplier.get()

    /**
     * Returns the wrapped value, or throws the exception supplied by the given Supplier if absent.
     */
    fun <X : Throwable?> orElseThrow(exceptionSupplier: Supplier<out X?>): T =
        value ?: throw exceptionSupplier.get()!!

    companion object {
        @Serial
        private const val serialVersionUID = 7369716185425581870L

        /**
         * Static factory to create a wrapper, supporting null values.
         */
        fun <T> of(value: T?): CacheValueWrapper<T?> = CacheValueWrapper(value)

        /**
         * Static factory to create an empty wrapper.
         */
        fun <T> empty(): CacheValueWrapper<T?> = CacheValueWrapper(null)
    }
}

