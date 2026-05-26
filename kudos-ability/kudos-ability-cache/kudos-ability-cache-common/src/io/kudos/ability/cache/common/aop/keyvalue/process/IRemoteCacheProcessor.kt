package io.kudos.ability.cache.common.aop.keyvalue.process

/**
 * Low-level remote cache processor: the aspects for [TenantAdvancedCacheable] / [TenantAdvancedCacheEvict]
 * call remote storage directly through this interface (typical implementation goes through Redis), bypassing
 * Spring's local CacheManager.
 *
 * The three methods correspond to the standard get / put / evict operations; the `cacheKey + dataKey`
 * two-level structure supports the Redis hash form (cacheKey as the hash key, dataKey as the field name).
 *
 * @author K
 * @since 1.0.0
 */
interface IRemoteCacheProcessor {
    /**
     * Retrieves cached data.
     * @param cacheKey
     * @param dataKey
     * @return
     */
    fun getCacheData(cacheKey: String, dataKey: String): Any?

    /**
     * Writes cached data.
     * @param cacheKey
     * @param dataKey
     * @param o
     * @param timeOut
     */
    fun writeCacheData(cacheKey: String, dataKey: String, o: Any?, timeOut: Long)

    /**
     * Clears the cache.
     * @param cacheKey
     * @param s
     * @param b
     */
    fun clearCache(cacheKey: String, s: String, b: Boolean)
}
