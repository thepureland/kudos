package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.CacheItemInitializing
import io.kudos.ability.cache.common.support.CacheConfig
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * Cache manager interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IKeyValueCacheManager<T : Cache> : CacheManager, CacheItemInitializing {

    /**
     * Creates a cache instance from the given cache configuration.
     *
     * @param cacheConfig cache configuration
     * @return cache instance
     * @author K
     * @since 1.0.0
     */
    fun createCache(cacheConfig: CacheConfig): T

    /**
     * Deletes all keys matching the pattern under the given cacheName (uses SCAN instead of KEYS).
     *
     * @param cacheName Spring Cache name
     * @param pattern   business key pattern, e.g., "user:*"
     */
    fun evictByPattern(cacheName: String, pattern: String)

    /**
     * Checks whether the given key exists in the cache (independent of whether the value is null).
     *
     * @param cacheName Spring Cache name
     * @param key       cache key
     * @return true if present; false otherwise
     */
    fun existsKey(cacheName: String, key: Any): Boolean

}