package io.kudos.ability.cache.common.enums

import io.kudos.base.enums.ienums.IDictEnum


/**
 * Cache operation type enum.
 *
 * Used by cache admin / monitoring pages to dispatch "perform this operation on this cache" commands;
 * this module's own AOP flow does not consume the enum directly (it goes through annotations like
 * `@TenantCacheable` / `@TenantCacheEvict`).
 *
 * @author K
 * @since 1.0.0
 */
enum class CacheHandleType(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    /** Reload the cache entry for the given key. */
    OVERLOAD("overload", "Reload cache entry for the given key"),

    /** Reload all keys in the entire cache. */
    OVERLOAD_ALL("overloadAll", "Reload all cache entries"),

    /** Evict the cache entry for the given key. */
    EVICT("evict", "Evict cache entry for the given key"),

    /** Clear the entire cache. */
    EVICT_ALL("evictAll", "Clear all cache entries"),

    /** Check whether the key exists (does not return the value). */
    GET_KEY("getKey", "Check whether key exists"),

    /** Get the value for the given key. */
    GET_VALUE("getValue", "Get value for the given key");

    companion object {
        /** Look up the enum by [code] literal; returns null when no match. */
        fun get(code: String): CacheHandleType? = entries.firstOrNull { it.code == code }
    }

}
