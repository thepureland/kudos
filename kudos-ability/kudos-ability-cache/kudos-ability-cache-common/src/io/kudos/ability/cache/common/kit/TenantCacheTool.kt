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
 * - Depends on tenantId from KudosContext; with no tenant present, keys land in the reserved
 *   [TenantCacheTool.NO_TENANT_NAMESPACE] rather than sharing a namespace with a tenant named "null".
 * - All cache operations apply tenant isolation automatically — do not add the tenant prefix manually.
 * - The clear-all operation removes data for all tenants — use with care.
 */
object TenantCacheTool {

    /** Separator between the tenant namespace and the business key. */
    const val TENANT_KEY_SEPARATOR = "::"

    /**
     * Namespace used for cache keys built while no tenant is present in the context.
     *
     * [getTenantKey] used to interpolate the tenant id straight into `"$tenantId::$key"`, so a null tenant
     * produced the literal prefix `"null"`. That is not a reserved value: `"null"` is a perfectly legal
     * tenant id, so a tenant actually named `null` shared one keyspace with every no-tenant caller and the
     * two could read each other's entries. This token cannot be taken by a real tenant — [getTenantKey]
     * rejects a tenant id equal to it.
     *
     * Note this changes the key layout for no-tenant entries (`null::k` -> `__kudos_no_tenant__::k`).
     * Existing entries are not migrated; they simply go unread and age out by TTL.
     */
    const val NO_TENANT_NAMESPACE = "__kudos_no_tenant__"

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
     * - With no tenant in context the reserved [NO_TENANT_NAMESPACE] is used, never the literal "null".
     * - The "::" separator distinguishes the tenant id from the original key.
     * - All cache operations apply this method automatically.
     *
     * @param key original cache key
     * @return the cache key prefixed with the tenant namespace
     * @throws IllegalStateException when the tenant id could impersonate another tenant's namespace
     */
    private fun getTenantKey(key: Any): String {
        val tenantId = KudosContextHolder.get().tenantId
        val namespace = when {
            tenantId.isNullOrBlank() -> NO_TENANT_NAMESPACE
            // A tenant id carrying the separator can forge any other tenant's prefix: tenant "a::b" writing
            // key "k" and tenant "a" writing key "b::k" both resolve to "a::b::k". Fail fast — this can only
            // come from a malformed tenant id, and silently producing forgeable keys is the worse outcome.
            tenantId.contains(TENANT_KEY_SEPARATOR) -> throw IllegalStateException(
                "tenantId must not contain '$TENANT_KEY_SEPARATOR' (it would collide with another tenant's " +
                    "cache namespace): $tenantId"
            )
            tenantId == NO_TENANT_NAMESPACE -> throw IllegalStateException(
                "tenantId must not equal the reserved no-tenant namespace '$NO_TENANT_NAMESPACE'"
            )
            else -> tenantId
        }
        return "$namespace$TENANT_KEY_SEPARATOR$key"
    }
}
