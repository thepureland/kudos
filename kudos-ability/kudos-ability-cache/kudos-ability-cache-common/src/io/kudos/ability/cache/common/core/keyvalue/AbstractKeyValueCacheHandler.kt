package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.AbstractCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory

/**
 * Abstract handler for key-value caches.
 *
 * @param T cache item type
 * @author K
 * @since 1.0.0
 */
abstract class AbstractKeyValueCacheHandler<T> : AbstractCacheHandler<T>() {

    /**
     * Checks whether the given cache key exists.
     *
     * @param key cache key
     * @return true if present in the cache; false otherwise
     */
    open fun isExists(key: String): Boolean {
        return value(key) != null
    }

    /**
     * Returns the value for the given cache key.
     *
     * @param key cache key
     * @return value associated with the cache key
     */
    open fun value(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return KeyValueCacheKit.getValue(cacheName(), key) as T?
    }

    /**
     * Evicts the cache entry for the given key.
     *
     * @param key cache key
     */
    open fun evict(key: String) {
        KeyValueCacheKit.evict(cacheName(), key)
        log.info("Manually evicted cache entry in ${cacheName()} for key ${key}.")
    }

    /**
     * Clears all cache entries.
     */
    open fun clear() {
        KeyValueCacheKit.clear(cacheName())
        log.info("Manually cleared all entries in cache ${cacheName()}.")
    }

    /**
     * Reloads the cache entry for the given key.
     *
     * @param key cache key
     */
    open fun reload(key: String) {
        evict(key)
        log.info("Manually reloading cache ${cacheName()} for key ${key}...")
        val role = doReload(key)
        if (role == null) {
            log.info("No corresponding data exists in the database!")
        } else {
            log.info("Reload succeeded.")
        }
    }

    /**
     * Performs the reload for the given cache key.
     *
     * @param key cache key
     * @return value associated with the cache key. If not found, returns an empty collection for collection types or null otherwise.
     */
    protected abstract fun doReload(key: String): T?

    private val log = LogFactory.getLog(this::class)

}