package io.kudos.ability.data.rdb.jdbc.datasource

import java.util.concurrent.ConcurrentHashMap

/**
 * Resolution cache for dynamic routing: maps a routing cache key (built by
 * [io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool.convertCacheMapKey]) to the real data
 * source key registered in the baomidou routing table.
 *
 * Owned by neither the aspect nor the processor: [io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceInterceptor]
 * reads through it, and [DsContextProcessor.refreshDatasource] invalidates it — both depend on
 * this component instead of on each other's static state.
 *
 * Concurrency model: lock-free. Two threads resolving the same key concurrently may both run the
 * resolver; [DsContextProcessor] serializes the expensive part (data source creation) with a
 * per-key lock, so the duplicate resolution is a cheap lookup and only one value wins
 * `putIfAbsent`. A `null` resolution is never cached — "no context yet" and "console tenant" are
 * transient states that must be re-evaluated on the next call.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RoutingCache {

    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Returns the cached data source key for [key], running [resolver] and caching its non-null
     * result on a miss. Returns `null` when the resolver decides the current call should not
     * switch data sources (nothing is cached in that case).
     */
    fun resolve(key: String, resolver: (String) -> String?): String? =
        cache[key] ?: resolver(key)?.also { cache.putIfAbsent(key, it) }

    /**
     * Drops every cached resolution so the next lookup re-runs its resolver. Call after a
     * tenant's data source is dynamically added / modified.
     */
    fun invalidateAll() {
        cache.clear()
    }

    /** Number of cached resolutions (monitoring / tests). */
    fun size(): Int = cache.size
}
