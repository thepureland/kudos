package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.Cache
import kotlin.reflect.KClass

/**
 * Tenant cache tool.
 *
 * Provides cache utilities for multi-tenant environments, supporting tenant-level cache isolation.
 *
 * Core capabilities:
 * 1. Tenant key generation: automatically prefixes cache keys with the tenant id, in the form "tenantId::originalKey".
 * 2. Cache isolation: ensures different tenants' cached data are isolated from each other via tenant keys.
 * 3. Cache operations: provides a complete cache operation interface (get, put, evict, clear, etc.).
 * 4. Pattern eviction: supports deleting cache entries by pattern, automatically prepending the tenant prefix.
 *
 * Tenant key mechanism:
 * - All cache operations automatically read the current tenant id from KudosContext.
 * - A "tenantId::" prefix is prepended to the original key, producing a tenant-isolated key.
 * - Example: tenant id "1001", original key "user:123", actual key "1001::user:123".
 *
 * Use cases:
 * - Multi-tenant SaaS applications that require data isolation between tenants.
 * - Shared cache storage that still needs per-tenant data isolation.
 * - Avoiding cross-tenant cache interference.
 *
 * Caveats:
 * - Depends on tenantId from KudosContext; an empty tenantId may produce an unexpected cache key.
 * - All cache operations apply tenant isolation automatically — do not add the tenant prefix manually.
 * - The clear-all operation removes data for all tenants — use with care.
 */
object TenantCacheTool {

    /**
     * Whether caching is enabled. Both the global switch and the cache-specific switch must be on.
     *
     * @param cacheName cache name
     * @return true: enabled; false: disabled
     * @author K
     * @since 1.0.0
     */
    fun isCacheActive(cacheName: String): Boolean = KeyValueCacheKit.isCacheActive(cacheName)

    /**
     * Returns the cache by name.
     *
     * @param name cache name
     * @return the cache object
     * @author K
     * @since 1.0.0
     */
    fun getCache(name: String): Cache? = KeyValueCacheKit.getCache(name)

    /**
     * Returns the value of the given key in the specified cache.
     *
     * @param cacheName  cache name
     * @param key        cache key
     * @param valueClass type of the value for the cache key
     * @return the value associated with the cache key
     * @author K
     * @since 1.0.0
     */
    fun <T: Any> getValue(cacheName: String, key: Any, valueClass: KClass<T>): T? =
        KeyValueCacheKit.getValue(cacheName, getTenantKey(key), valueClass)

    /**
     * Returns the value of the given key in the specified cache.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @return the value associated with the cache key
     * @author K
     * @since 1.0.0
     */
    fun getValue(cacheName: String, key: Any): Any? =
        KeyValueCacheKit.getValue(cacheName, getTenantKey(key))

    /**
     * Writes a value to the cache.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @param value     value to cache
     * @author K
     * @since 1.0.0
     */
    fun put(cacheName: String, key: Any, value: Any?) {
        KeyValueCacheKit.put(cacheName, getTenantKey(key), value)
    }

    /**
     * Writes a value to the cache if it does not already exist.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @param value     value to cache
     * @author K
     * @since 1.0.0
     */
    fun putIfAbsent(cacheName: String, key: Any, value: Any?) {
        KeyValueCacheKit.putIfAbsent(cacheName, getTenantKey(key), value)
    }

    /**
     * Evicts a cache entry and triggers a dependency notification.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @author K
     * @since 1.0.0
     */
    fun evict(cacheName: String, key: Any) {
        doEvict(cacheName, key)
    }

    /**
     * Evicts a cache entry.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @author K
     * @since 1.0.0
     */
    fun doEvict(cacheName: String, key: Any) {
        KeyValueCacheKit.doEvict(cacheName, getTenantKey(key))
    }

    /**
     * Clears all keys for the **current tenant** under the given cache (via the `{tenantId}::*` pattern).
     *
     * The old implementation called [KeyValueCacheKit.clear] directly, wiping data for all tenants — despite
     * having "Tenant" in its name, the semantics were cross-tenant, and this was an actual incident source.
     * The current implementation is narrowed to the current tenant only; to truly clear across all tenants,
     * call [clearAllTenants] explicitly.
     *
     * @param cacheName cache name
     */
    fun clear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        KeyValueCacheKit.evictByPattern(cacheName, getTenantKey("*"))
    }

    /**
     * Clears all keys for the **current tenant** under the given cache (without notification).
     * Semantically equivalent to [clear]; differs only in whether the eviction is broadcast.
     *
     * @param cacheName cache name
     */
    fun doClear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        KeyValueCacheKit.evictByPattern(cacheName, getTenantKey("*"))
    }

    /**
     * Clears all data for **all tenants** under the given cache. Makes the semantics explicit so that
     * [clear] is not mistakenly used as a cross-tenant clear.
     * Use only when a cross-tenant clear is truly required (e.g. cache schema upgrades, config rollbacks).
     *
     * @param cacheName cache name
     */
    fun clearAllTenants(cacheName: String) {
        KeyValueCacheKit.clear(cacheName)
    }

    /**
     * Whether the cache is written back immediately after an insert or update.
     *
     * @param cacheName cache name
     * @return true: write back immediately; otherwise false. Returns false when the cache does not exist.
     * @author K
     * @since 1.0.0
     */
    fun isWriteInTime(cacheName: String): Boolean = KeyValueCacheKit.isWriteInTime(cacheName)

    /**
     * Returns the configuration of the cache with the given name.
     *
     * @param cacheName cache name
     * @return the cache configuration; null if not found
     * @author K
     * @since 1.0.0
     */
    fun getCacheConfig(cacheName: String): CacheConfig? = KeyValueCacheKit.getCacheConfig(cacheName)

    /**
     * Reloads a cache entry.
     *
     * @param cacheName cache name
     * @param key       key
     */
    fun reload(cacheName: String, key: String) {
        KeyValueCacheKit.reload(cacheName, getTenantKey(key))
    }

    /**
     * Evicts cache entries whose keys start with the given prefix.
     * @param cacheName cache name
     * @param keyPattern key prefix
     */
    fun evictByPattern(cacheName: String, keyPattern: String) {
        if (!isCacheActive(cacheName)) return
        KeyValueCacheKit.evictByPattern(cacheName, getTenantKey(keyPattern))
    }

    /**
     * Reloads all entries in the given cache.
     *
     * @param cacheName cache name
     */
    fun reloadAll(cacheName: String) {
        KeyValueCacheKit.reloadAll(cacheName)
    }

    /**
     * Builds a tenant-isolated cache key.
     *
     * Prepends the tenant id to the original key, providing cache isolation in multi-tenant environments.
     *
     * Workflow:
     * 1. Acquire the tenant id: read the current thread's tenant id from KudosContext.
     * 2. Concatenate the key: build a "tenantId::originalKey" string.
     * 3. Return the result: return the tenant-isolated key.
     *
     * Key format:
     * - Format: "{tenantId}::{originalKey}"
     * - Example: tenant id "1001", original key "user:123", result "1001::user:123"
     *
     * Tenant isolation:
     * - The same key under different tenants resolves to different cache keys.
     * - Ensures cached data from different tenants is isolated.
     * - Prevents cross-tenant cache interference.
     *
     * Caveats:
     * - When tenantId is null, "null" is used as the prefix.
     * - The "::" separator distinguishes the tenant id from the original key.
     * - All cache operations apply this method automatically.
     *
     * @param key original cache key
     * @return the cache key prefixed with the tenant id
     */
    private fun getTenantKey(key: Any): String {
        val tenantId = KudosContextHolder.get().tenantId
        return "$tenantId::$key"
    }
}
