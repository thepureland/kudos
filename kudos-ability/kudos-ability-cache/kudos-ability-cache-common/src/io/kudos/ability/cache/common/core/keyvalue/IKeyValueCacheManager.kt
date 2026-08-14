package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.CacheItemInitializing
import io.kudos.ability.cache.common.support.CacheConfig
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * Cache manager interface.
 *
 * ## Naming contract
 *
 * Every `cacheName` on this interface is the **logical** name — the one written in `cache-items`, with no
 * version prefix. Applying [io.kudos.ability.cache.common.init.properties.CacheVersionConfig] is the
 * implementation's job, because only the implementation knows where the prefix belongs in its own storage
 * layout (Caffeine puts it in the registered cache name, Redis puts it in the key prefix).
 *
 * A `pattern` is always a **raw key pattern** and never carries a version prefix: it is matched against cache
 * *keys*, and keys are stored unprefixed in both tiers.
 *
 * This used to be unstated, and the two implementations drifted apart — Caffeine expected an already-prefixed
 * name while Redis expected a logical one, and `evictByPattern` / `existsKey` disagreed even within the same
 * class. Callers compensated site by site, and one of them prefixed the *pattern*, which silently matched
 * nothing as soon as a cache version was configured. State the contract once here; implementations normalize
 * internally.
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
     * Deletes all keys matching the pattern under the given cacheName.
     *
     * Implementations scan their own keyspace; the Redis one currently issues `KEYS`, which blocks the server
     * on large keyspaces — see `RedisKeyValueCacheManager.evictByPattern`.
     *
     * @param cacheName logical cache name, without version prefix (see the naming contract on this interface)
     * @param pattern   raw business key pattern, e.g. `"user:*"`; never version-prefixed
     */
    fun evictByPattern(cacheName: String, pattern: String)

    /**
     * Checks whether the given key exists in the cache (independent of whether the value is null).
     *
     * @param cacheName logical cache name, without version prefix (see the naming contract on this interface)
     * @param key       cache key
     * @return true if present; false otherwise
     */
    fun existsKey(cacheName: String, key: Any): Boolean

    /**
     * Looks up many keys at once.
     *
     * Exists so that batch callers stop paying one round trip per key: `@BatchCacheable` reads every key it was
     * asked for, and doing that one `get` at a time turns a single batch call into N Redis round trips, which
     * cancels out most of what batching is for.
     *
     * **Presence, not nullness, marks a hit.** The returned map contains an entry only for keys that were found,
     * and that entry's value may itself be null when a null was deliberately cached (negative caching). Callers
     * must therefore branch on `containsKey`, not on `get(key) != null` — otherwise a cached null reads as a
     * miss and the source gets hit again for exactly the keys the cache is meant to shield it from.
     *
     * @param cacheName logical cache name, without version prefix (see the naming contract on this interface)
     * @param keys      keys to look up
     * @return map of found keys to their (possibly null) values; keys absent from the cache are absent from the map
     */
    fun multiGet(cacheName: String, keys: Collection<Any>): Map<Any, Any?>

}