package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import org.springframework.cache.caffeine.CaffeineCache
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Caffeine implementation of the K-V local cache manager.
 *
 * Translates [CacheConfig] into a Caffeine spec plus per-cache `expireAfterWrite(ttl)`, then wraps
 * the result in [DrainingCaffeineCache] to work around the "still readable after clear" issue
 * caused by `invalidateAll()` being applied asynchronously.
 *
 * [ensureSizeBound] adds a safety-net `maximumSize` to the caffeine spec so a missing upper bound
 * in business config cannot cause a long-running OOM — this is a real production issue we have hit;
 * if you see the warn log, please fill in the spec.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CaffeineKeyValueCacheManager : AbstractKeyValueCacheManager<CaffeineCache>() {

    /**
     * Build a single [CaffeineCache] from [cacheConfig]: merge the global spec, override the ttl,
     * and apply the versioned prefix.
     * Failing preconditions throw immediately via `requireNotNull` — typically yml missing
     * `spring.cache.caffeine.spec` or a cache item missing `ttl`; failing at startup is better
     * than discovering misconfiguration at runtime.
     */
    override fun createCache(cacheConfig: CacheConfig): CaffeineCache {
        val spec = requireNotNull(properties.caffeine.spec) { "caffeine spec is required" }
        val effectiveSpec = ensureSizeBound(spec)
        val cacheBuilder = Caffeine.from(effectiveSpec)
        val ttlSec = requireNotNull(cacheConfig.ttl) { "cache ttl is required" }.toLong()
        cacheBuilder.expireAfterWrite(ttlSec, TimeUnit.SECONDS)
        var name = requireNotNull(cacheConfig.name) { "cache name is required" }
        val ignoreVersion: Boolean? = cacheConfig.ignoreVersion
        if (ignoreVersion == null || !ignoreVersion) {
            name = versionConfig.getFinalCacheName(name)
        }
        val caffeineCache = DrainingCaffeineCache(name, cacheBuilder.build())
        log.debug("Initialized local cache [{0}] successfully!", name)
        return caffeineCache
    }

    /**
     * Add a safety-net size bound to a caffeine spec.
     * Do not rely on every business owner to set maximumSize correctly in every environment's
     * config file: a single missing spec means an unbounded LinkedHashMap and an inevitable OOM
     * over time. Silently add a fallback at creation time and emit a warn log.
     */
    private fun ensureSizeBound(spec: String): String {
        val hasSizeBound = SIZE_BOUND_REGEX.containsMatchIn(spec)
        if (hasSizeBound) return spec
        log.warn(
            "Caffeine spec does not configure maximumSize/maximumWeight; defaulting to [{0}] to prevent unbounded growth. Please set an explicit upper bound in caffeine.spec. Original spec=[{1}]",
            DEFAULT_MAX_SIZE,
            spec
        )
        return if (spec.isBlank()) "maximumSize=$DEFAULT_MAX_SIZE" else "$spec,maximumSize=$DEFAULT_MAX_SIZE"
    }

    /**
     * Evict cache entries by a wildcard `*` pattern.
     *
     * The argument may be a logical pattern (`user:*`) or an actual pattern that already carries a
     * version prefix (`v2:user:*`) — uniformly strip the version and re-add it to avoid duplicated
     * prefixes. Convert `*` into a regex `.*`, scan the entire nativeMap, and call
     * `cache.evict(key)` on every hit so the eviction takes effect immediately
     * ([DrainingCaffeineCache] performs a synchronous cleanUp).
     *
     * **Performance note**: full scan plus per-key evict; not cheap when the cache holds many
     * entries. Evaluate hot spots before using in production.
     */
    override fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName) ?: return
        val realPattern: String = versionConfig.getFinalCacheName(versionConfig.getRealCacheName(pattern))
        val nativeCache = (cache as CaffeineCache).nativeCache.asMap()
        val regex = "^" + realPattern.replace("*", ".*") + "$"
        val p = Pattern.compile(regex)
        for (key in nativeCache.keys) {
            if (p.matcher(key.toString()).matches()) {
                cache.evict(key)
            }
        }
    }

    /**
     * Whether the given key exists — checked directly via `nativeCache.asMap().containsKey`,
     * without triggering Caffeine's LoadingCache load logic (distinct from [CaffeineCache.get]
     * semantics).
     */
    override fun existsKey(cacheName: String, key: Any): Boolean {
        val cache = getCache(cacheName) ?: return false
        return (cache as CaffeineCache).nativeCache.asMap().containsKey(key)
    }

    private val log = LogFactory.getLog(this::class)

    companion object {
        /** Fallback size: not strictly scientific, but better than unbounded; users should override explicitly in their spec to match their workload. */
        private const val DEFAULT_MAX_SIZE = 10_000L

        /** Match size-related options in a caffeine spec (`=` must follow the key directly to avoid matching things like maximumWeightXxx). */
        private val SIZE_BOUND_REGEX = Regex("\\b(maximumSize|maximumWeight)\\s*=")
    }

}
