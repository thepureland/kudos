package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.NullValue
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
     * The pattern is matched against keys **as given**. It used to be run through
     * `getFinalCacheName(getRealCacheName(pattern))`, which prepends the *cache name* version prefix — but the
     * version lives in the registered cache name (see [createCache]), never in the keys held by that cache's
     * native map. With any non-blank `kudos.ability.cache.version`, a pattern like `1001::*` became
     * `v2::1001::*` and matched nothing: pattern eviction silently turned into a no-op. It went unnoticed only
     * because the shipped yml leaves the version blank, which makes the prefixing a no-op too.
     *
     * Convert `*` into a regex `.*`, scan the entire nativeMap, and call `cache.evict(key)` on every hit so the
     * eviction takes effect immediately ([DrainingCaffeineCache] performs a synchronous cleanUp).
     *
     * **Performance note**: full scan plus per-key evict; not cheap when the cache holds many
     * entries. Evaluate hot spots before using in production.
     */
    override fun evictByPattern(cacheName: String, pattern: String) {
        val cache = resolveCache(cacheName) ?: return
        val nativeCache = cache.nativeCache.asMap()
        // Quote the literal fragments between wildcards: keys may legally contain regex metacharacters
        // (".", "[", "(", "+", ...) — splicing them unescaped over-matches or breaks Pattern.compile.
        val regex = "^" + pattern.split("*").joinToString(".*") { Pattern.quote(it) } + "$"
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
     *
     * Caveat: Caffeine expires entries lazily, so an entry that is past its TTL but not yet evicted still
     * reports as present here; a `get` immediately afterwards can return null.
     */
    override fun existsKey(cacheName: String, key: Any): Boolean {
        val cache = resolveCache(cacheName) ?: return false
        return cache.nativeCache.asMap().containsKey(key)
    }

    /**
     * Bulk lookup via Caffeine's native `getAllPresent`, which walks the map once instead of doing one
     * lookup per key.
     *
     * Caffeine stores a cached null as Spring's `NullValue` sentinel (that is how [CaffeineCache] represents
     * "present but null"), so it is translated back here: the key stays in the result map — it *was* a hit —
     * with a null value.
     */
    override fun multiGet(cacheName: String, keys: Collection<Any>): Map<Any, Any?> {
        if (keys.isEmpty()) return emptyMap()
        val cache = resolveCache(cacheName) ?: return emptyMap()
        val present = cache.nativeCache.getAllPresent(keys)
        if (present.isEmpty()) return emptyMap()
        val result = LinkedHashMap<Any, Any?>(present.size)
        // Preserve the caller's key order; getAllPresent returns its own map ordering.
        keys.forEach { key ->
            if (present.containsKey(key)) {
                val stored = present[key]
                result[key] = if (stored is NullValue) null else stored
            }
        }
        return result
    }

    /**
     * Resolves a cache from the **logical** name required by [IKeyValueCacheManager], applying the version
     * prefix that [createCache] registers under.
     *
     * Falls back to the raw name so two other cases keep working: caches configured with `ignoreVersion=true`
     * are registered unprefixed, and callers still passing an already-prefixed name (the pre-contract
     * behaviour) resolve rather than silently missing.
     */
    private fun resolveCache(cacheName: String): CaffeineCache? {
        val prefixed = versionConfig.getFinalCacheName(cacheName)
        val cache = getCache(prefixed) ?: getCache(cacheName)
        return cache as? CaffeineCache
    }

    private val log = LogFactory.getLog(this::class)

    companion object {
        /** Fallback size: not strictly scientific, but better than unbounded; users should override explicitly in their spec to match their workload. */
        private const val DEFAULT_MAX_SIZE = 10_000L

        /** Match size-related options in a caffeine spec (`=` must follow the key directly to avoid matching things like maximumWeightXxx). */
        private val SIZE_BOUND_REGEX = Regex("\\b(maximumSize|maximumWeight)\\s*=")
    }

}
