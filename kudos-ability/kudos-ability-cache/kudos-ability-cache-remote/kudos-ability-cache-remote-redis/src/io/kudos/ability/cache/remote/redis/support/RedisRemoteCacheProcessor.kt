package io.kudos.ability.cache.remote.redis.support

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
import io.kudos.ability.data.memdb.redis.RedisTemplates
import java.time.Duration

/**
 * Redis remote cache processor.
 * Implements IRemoteCacheProcessor to provide read/write of remote cache backed by a Redis Hash.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisRemoteCacheProcessor(private val redisTemplates: RedisTemplates) : IRemoteCacheProcessor {

    /**
     * Reads cache data.
     *
     * Reads cached data for the given key from the Redis Hash.
     *
     * Data structure:
     * - Uses a Redis Hash to store cache data
     * - cacheKey is the Hash key, dataKey is the field within the Hash
     * - Multiple cache entries can be stored under the same Hash
     *
     * @param cacheKey primary cache key (Hash key)
     * @param dataKey  entry key within the cache (Hash field) to read
     * @return cached value, or null when absent
     */
    override fun getCacheData(cacheKey: String, dataKey: String): Any? {
        return redisTemplates.defaultRedisTemplate.opsForHash<Any?, Any?>().get(cacheKey, dataKey)
    }

    /**
     * Writes cache data.
     *
     * Writes data into a Redis Hash and sets the expiration.
     *
     * Workflow:
     * 1. If the value is null, do nothing.
     * 2. Write into the Hash: cacheKey as the Hash key, dataKey as the field, o as the value.
     * 3. Set the Hash expiration via the `expire` command (applies to the whole Hash).
     *
     * Data structure:
     * - Uses a Redis Hash to store cache data
     * - cacheKey is the Hash key, dataKey is the field, o is the value
     * - Multiple cache entries can be stored in the same Hash and share one expiration
     *
     * Expiration:
     * - Applies to the entire Hash, not a single field
     * - Any other fields in the Hash also share this expiration
     * - Time unit: milliseconds
     *
     * Notes:
     * - When the value is null, nothing is written and no expiration is set
     * - The expiration overwrites any previously set expiration
     * - Using a Hash structure stores multiple related cache entries efficiently
     *
     * @param cacheKey primary cache key (Hash key)
     * @param dataKey  entry key (Hash field)
     * @param o        value to cache (Hash value); a null value is a no-op
     * @param timeOut  cache expiration in milliseconds
     */
    override fun writeCacheData(cacheKey: String, dataKey: String, o: Any?, timeOut: Long) {
        val defaultRedisTemplate = redisTemplates.defaultRedisTemplate
        if (o != null) {
            redisTemplates.defaultRedisTemplate.opsForHash<Any?, Any?>().put(cacheKey, dataKey, o)
            defaultRedisTemplate.expire(cacheKey, Duration.ofMillis(timeOut))
        }
    }

    /**
     * Clears the cache.
     *
     * Clears the whole cache or a single key depending on the parameters.
     *
     * Workflow:
     * 1. Return early when cacheKey is blank.
     * 2. Decide the clearing mode by parameter b:
     *    - b=true: clear the entire cache (delete the whole Hash)
     *    - b=false and s is non-blank: clear one entry (delete the given field from the Hash)
     *    - b=false and s is blank: no-op
     *
     * Clearing modes:
     * - Whole cache: `delete` the entire cacheKey (Hash)
     * - Specific key: Hash `delete` of the given field (s) under cacheKey
     *
     * Data structure:
     * - Uses a Redis Hash to store cache data
     * - cacheKey is the Hash key, s is the Hash field
     * - Multiple cache entries can be stored in the same Hash
     *
     * Notes:
     * - When cacheKey is blank, returns immediately and does nothing
     * - Clearing the whole cache removes all data in the Hash; use with caution
     * - Clearing a specific key only removes the given field and does not affect other data
     *
     * @param cacheKey primary cache key (Hash key)
     * @param s        entry key (Hash field) to clear; ignored when b=true
     * @param b        whether to clear the whole cache; true = whole cache, false = single key
     */
    override fun clearCache(cacheKey: String, s: String, b: Boolean) {
        if (cacheKey.isBlank()) {
            return
        }
        if (b) {
            redisTemplates.defaultRedisTemplate.delete(cacheKey)
        } else if (s.isNotBlank()) {
            redisTemplates.defaultRedisTemplate.opsForHash<Any?, Any?>().delete(cacheKey, s)
        }
    }

}
