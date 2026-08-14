package io.kudos.ability.cache.common.support

import java.io.Serial
import java.io.Serializable
import java.util.function.Supplier

/**
 * Cache value wrapper — distinguishes "looked it up, the value is null" from "not in the cache at all".
 *
 * Spring's `Cache.ValueWrapper` expresses the same idea inside the cache API; this class is the **business-side**
 * counterpart, for callers of `KeyValueCacheKit` and friends that need the distinction a plain `T?` cannot carry.
 * That difference is what makes negative caching work: a cached null is an answer ("the source has nothing for
 * this key, do not ask again until it expires"), whereas an absent entry is a cache miss.
 *
 * **Presence is tracked explicitly, not inferred from the value.** The wrapper previously derived
 * `isPresent` from `value != null`, which collapsed the two states it exists to tell apart: `of(null)` and
 * `empty()` produced indistinguishable instances, so the documented purpose could not actually be served. The
 * flag is now carried in its own field:
 *
 * | Factory      | `isPresent` | `value` | Meaning                                  |
 * |--------------|-------------|---------|------------------------------------------|
 * | `of(v)`      | `true`      | `v`     | cached, value is `v`                     |
 * | `of(null)`   | `true`      | `null`  | cached, value is null (negative caching) |
 * | `empty()`    | `false`     | `null`  | not cached — go to the source            |
 *
 * Consequently [orElse] / [orElseGet] / [orElseThrow] branch on **presence**, matching `java.util.Optional`'s
 * treatment of an empty optional: a deliberately cached null is returned as null rather than replaced by the
 * fallback, since substituting it would defeat the negative caching it represents.
 *
 * @author K
 * @since 1.0.0
 */
class CacheValueWrapper<T> private constructor(
    /**
     * The wrapped value. `null` here is ambiguous on its own — pair it with [isPresent] to tell a cached null
     * from a miss.
     */
    val value: T?,

    /** Whether the cache held an entry for the key, regardless of whether that entry's value was null. */
    val isPresent: Boolean
) : Serializable {

    /** True when there was no cache entry at all; the caller should fall back to the source. */
    val isEmpty: Boolean
        get() = !isPresent

    /**
     * Returns the wrapped value when an entry was present (including a present null), otherwise [defaultValue].
     */
    fun orElse(defaultValue: T?): T? = if (isPresent) value else defaultValue

    /**
     * Returns the wrapped value when an entry was present (including a present null), otherwise the value
     * supplied by [supplier].
     */
    fun orElseGet(supplier: Supplier<out T?>): T? = if (isPresent) value else supplier.get()

    /**
     * Returns the wrapped value when an entry was present, otherwise throws the exception from
     * [exceptionSupplier].
     *
     * Note that a present null is returned as null rather than throwing — the entry existed.
     */
    fun <X : Throwable?> orElseThrow(exceptionSupplier: Supplier<out X?>): T? =
        if (isPresent) value else throw exceptionSupplier.get()!!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val that = other as? CacheValueWrapper<*> ?: return false
        return isPresent == that.isPresent && value == that.value
    }

    override fun hashCode(): Int = 31 * (value?.hashCode() ?: 0) + isPresent.hashCode()

    override fun toString(): String = if (isPresent) "CacheValueWrapper[$value]" else "CacheValueWrapper.empty"

    companion object {
        @Serial
        private const val serialVersionUID = 7369716185425581870L

        /**
         * Wraps a value that **was** found in the cache. Passing null records a cached null, which is a
         * different state from [empty].
         */
        fun <T> of(value: T?): CacheValueWrapper<T?> = CacheValueWrapper(value, true)

        /** Represents a cache miss: no entry for the key. */
        fun <T> empty(): CacheValueWrapper<T?> = CacheValueWrapper(null, false)
    }
}
