package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.cache.support.NullValue
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

/**
 * Redis cache manager.
 *
 * Extends Spring's RedisCacheManager to support cache version management and multi-tenant cache isolation.
 *
 * Core features:
 * 1. Cache versioning: CacheVersionConfig adds a version prefix to cache names, enabling isolation and upgrade.
 * 2. Dynamic cache creation: builds RedisCache instances from CacheConfig, with custom TTL.
 * 3. Pattern eviction: deletes cache keys by pattern using SCAN instead of KEYS to avoid blocking Redis.
 * 4. Cache initialization: bulk-creates configured cache instances after system initialization.
 *
 * Naming rules:
 * - actual cache name = version prefix + original cache name
 * - e.g. version "v1", cache name "user" -> actual name "v1::user"
 *
 * Pattern eviction:
 * - Deletes keys by pattern, e.g. "user:*" removes all keys starting with "user:".
 * - Uses SCAN instead of KEYS to avoid blocking Redis in production.
 * - Automatically applies the cache name prefix and the version prefix.
 *
 * Notes:
 * - Version prefix is applied at creation time, ensuring isolation across cache versions.
 * - Custom TTL is supported; falls back to the default when unspecified.
 * - Created caches are appended to `caches` for later use.
 *
 * **Runtime cache creation is disabled.** `RedisCacheManager`'s two-arg constructor enables it, which made
 * `getCache("anything")` mint a cache on the spot — a plain [RedisCache] rather than [ScanClearRedisCache], with
 * no version prefix and the default TTL instead of the one configured for that item. `existsKey` goes through
 * `getCache`, so merely probing an unconfigured name planted such a cache in the manager. An unconfigured name
 * now resolves to null, which surfaces the misconfiguration instead of quietly working with the wrong settings.
 *
 * @param redisTemplate template for the redis instance this cache manager targets (the one selected by
 *   `kudos.ability.cache.remoteStore`), used by the `keys + delete` paths that work around Spring's broken
 *   `cacheWriter.clean`. Injected rather than looked up statically so that the eviction paths cannot end up on a
 *   different redis instance — or a different key serializer — than the one holding the data.
 * @param ttlJitterPercent per-entry TTL spread in percent; `0` keeps TTLs exact. See
 *   [io.kudos.ability.cache.remote.redis.init.RedisCacheProperties.ttlJitterPercent].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisKeyValueCacheManager(
    private val cacheWriter: RedisCacheWriter,
    private val defaultCacheConfiguration: RedisCacheConfiguration,
    private val redisTemplate: RedisTemplate<String, Any>? = null,
    private val ttlJitterPercent: Int = 0
) : RedisCacheManager(
    cacheWriter,
    defaultCacheConfiguration,
    false
), IKeyValueCacheManager<RedisCache> {

    init {
        // Fail at startup rather than silently producing nonsense expiries: above 100% the lower edge would
        // go negative, and even 50% is already a very wide spread for a cache TTL.
        require(ttlJitterPercent in 0..50) {
            "kudos.ability.cache.redis.ttl-jitter-percent must be within 0..50, got $ttlJitterPercent"
        }
    }

    var caches: MutableList<RedisCache> = mutableListOf()

    @Resource
    private lateinit var versionConfig: CacheVersionConfig

    override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) {
        cacheConfigMap.forEach { (key: String, cacheConfig: CacheConfig) ->
            val cache: RedisCache = createCache(cacheConfig)
            log.debug("Remote cache [{0}] initialized successfully.", key)
            addCache(cache)
        }
        afterPropertiesSet()
    }

    /**
     * Appends a single cache instance to the internal table. `@Synchronized` exists in case
     * "dynamic add cache at runtime" is needed later; today the only caller is `initCacheAfterSystemInit`
     * (single-threaded at startup), so there is no real concurrent contention.
     */
    @Synchronized
    fun addCache(cache: RedisCache) {
        this.caches.add(cache)
    }

    /**
     * Spring `AbstractCacheManager` template method returning all caches to be registered.
     * Note this returns the direct reference to [caches] — `afterPropertiesSet()` makes an internal copy
     * once called, after which subsequent `addCache` calls will not be picked up automatically.
     */
    override fun loadCaches(): MutableCollection<RedisCache> {
        return caches
    }

    /**
     * Creates a Redis cache instance.
     *
     * Builds a RedisCache from CacheConfig with custom TTL and version management.
     *
     * Workflow:
     * 1. Build a default RedisCacheConfiguration.
     * 2. Keep null-value caching enabled, matching the local tier (negative caching).
     * 3. Reuse the default key/value serializers.
     * 4. If a TTL is configured, set the cache expiration.
     * 5. Apply the version prefix to the cache name.
     * 6. Build and return the RedisCache instance.
     *
     * Config:
     * - TTL: when cacheConfig.ttl is set, used as the cache expiration (seconds).
     * - Serialization: reuses the default serializers so key/value serialization stays consistent.
     * - Versioning: the cache name receives the version prefix, e.g. "v1::user".
     *
     * Notes:
     * - With no TTL set, the default configuration is used (potentially no expiration).
     * - The version prefix is applied automatically, ensuring isolation across cache versions.
     *
     * @param cacheConfig cache configuration with name, TTL, etc.
     * @return the created RedisCache instance
     */
    override fun createCache(cacheConfig: CacheConfig): RedisCache {
        // Derive from the injected default rather than a fresh `defaultCacheConfig()`. Building from scratch
        // meant a cache item without an explicit `ttl` inherited `defaultCacheConfig()`'s zero TTL — i.e. it
        // never expired — instead of the 900s default configured in RedisCacheAutoConfiguration, contradicting
        // this method's own KDoc. It also dropped the key prefix, which `evictByPattern` reads back off the
        // default configuration: the two agreed only as long as nobody customized `prefixWith(...)`.
        // Null caching stays enabled (Spring's default) so this tier agrees with the local one; see
        // RedisCacheAutoConfiguration for why disabling it broke `@Cacheable` methods that return null.
        var redisCacheConfiguration = defaultCacheConfiguration
        cacheConfig.ttl?.let { ttl ->
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofSeconds(ttl.toLong()))
        }
        redisCacheConfiguration = withTtlJitter(redisCacheConfiguration)
        val realKey: String = versionConfig.getFinalCacheName(requireNotNull(cacheConfig.name) { "cache name required" })
        // Fix for the Spring Boot 4.0.6 bug where [RedisCache.clear] does not actually delete Redis keys:
        // our override uses `RedisTemplate.keys(pattern) + delete(keys)` directly, applied to every RedisCache instance.
        return ScanClearRedisCache(realKey, cacheWriter, redisCacheConfiguration, redisTemplate)
    }

    /**
     * Wraps [config]'s TTL function so each entry gets its own randomized expiry.
     *
     * Wrapping the existing `TtlFunction` rather than reading a fixed `Duration` means the jitter covers both
     * an explicitly configured per-cache TTL and the module default inherited from `defaultCacheConfiguration`;
     * reading the duration would have silently left default-TTL caches unjittered.
     *
     * @return [config] unchanged when jitter is disabled
     */
    private fun withTtlJitter(config: RedisCacheConfiguration): RedisCacheConfiguration {
        if (ttlJitterPercent <= 0) return config
        val delegate = config.ttlFunction
        return config.entryTtl(RedisCacheWriter.TtlFunction { key, value ->
            jitter(delegate.getTimeToLive(key, value))
        })
    }

    /**
     * Draws an expiry uniformly from `base * (1 ± ttlJitterPercent/100)`.
     *
     * A non-positive [base] is Spring's "never expires" marker ([RedisCacheWriter.TtlFunction.NO_EXPIRATION])
     * and is returned untouched — spreading out something that has no expiry would invent one.
     */
    private fun jitter(base: Duration): Duration {
        if (base.isZero || base.isNegative) return base
        val baseSeconds = base.seconds
        val span = baseSeconds * ttlJitterPercent / 100
        if (span <= 0L) return base // TTL too small for the percentage to move it by a whole second
        val jittered = baseSeconds + ThreadLocalRandom.current().nextLong(-span, span + 1)
        // Guard the lower edge: keep an entry that was meant to expire from becoming non-expiring.
        return if (jittered > 0L) Duration.ofSeconds(jittered) else Duration.ofSeconds(1)
    }

    /**
     * Deletes all keys in a cache by pattern.
     *
     * Workflow:
     * 1. `prefixProvider.compute(cacheName)` produces the cache-key prefix (default `"$cacheName::"`).
     * 2. [CacheVersionConfig.getFinalCacheName] applies the version prefix.
     * 3. Build the final match pattern: actual key prefix + business pattern (e.g. `*` / `user:*`).
     * 4. Use [RedisTemplate.keys] + [RedisTemplate.delete] to locate and delete.
     *
     * Example: version `v1`, cache `user`, pattern `*` -> final match `v1::user::*`.
     *
     * **Important**: the old implementation called `cacheWriter.clear(name, pattern)`, but under Spring Boot 4.0.6
     * that path does not actually delete keys with this project's Redis config (same bug fixed in
     * [ScanClearRedisCache] for `RedisCache.clear()`; see [ScanClearRedisCache] KDoc). Now goes directly through
     * `RedisTemplate`'s keys/delete API.
     *
     * **Limitations**: [RedisTemplate.keys] blocks Redis on large datasets; this project's cache key count is tiny
     * (hundreds), which is acceptable. If volume grows, switch to SCAN-based iterative deletion.
     *
     * @param cacheName Spring Cache name
     * @param pattern   business key pattern, supports wildcards
     */
    override fun evictByPattern(cacheName: String, pattern: String) {
        // Same reason as [ScanClearRedisCache.clear]: Spring Boot 4.0.6's `cacheWriter.clear(name, pattern)`
        // does not actually delete keys under our Redis config (see ScanClearRedisCache KDoc for root cause).
        // Use RedisTemplate.keys + delete through the standard API instead.
        val prefixProvider = defaultCacheConfiguration.keyPrefix
        val keyPrefix = prefixProvider.compute(cacheName)
        val realKey: String = versionConfig.getFinalCacheName(keyPrefix)
        val fullPattern = realKey + pattern
        // Prefer the injected template: a statically looked-up one may target another redis instance or use a
        // different key serializer, in which case the pattern bytes match nothing and eviction silently no-ops.
        val template = redisTemplate ?: findRedisTemplate() ?: return
        val matched = template.keys(fullPattern)
        if (matched.isNotEmpty()) {
            template.delete(matched)
        }
    }

    /**
     * Checks whether the given key exists — via the public `Cache.get(key)` (no reflection).
     *
     * History: the old implementation reflectively called `RedisCache.createAndConvertCacheKey`, which fails
     * under JPMS. The public API is simpler and more reliable: `get` returns a `ValueWrapper` when an entry
     * exists and `null` when it does not, so a non-null wrapper means the key is present.
     *
     * This holds independently of null caching, and in fact only became fully correct once null values were
     * allowed: a cached null is reported as a present entry (a wrapper *around* null) rather than being
     * impossible to store in the first place.
     *
     * Cost note: this deserializes the whole value just to test for presence. A `RedisCacheWriter`-level
     * `EXISTS` would avoid that; it is left as-is because the public API keeps the JPMS problem away.
     */
    override fun existsKey(cacheName: String, key: Any): Boolean {
        // Takes the logical name per the IKeyValueCacheManager contract and applies the version prefix here,
        // matching evictByPattern above. The two used to disagree — this one resolved the raw argument against
        // names registered as "<version>::<name>", so it only ever worked when the caller happened to pass an
        // already-prefixed name. The raw-name fallback covers callers that still do.
        val redisCache = resolveCache(cacheName) ?: return false
        return redisCache.get(key) != null
    }

    /** Resolves a cache from its logical name, tolerating an already-prefixed name (see [existsKey]). */
    private fun resolveCache(cacheName: String): RedisCache? {
        val prefixed = versionConfig.getFinalCacheName(cacheName)
        return (getCache(prefixed) ?: getCache(cacheName)) as? RedisCache
    }

    /**
     * Bulk lookup in a single `MGET` instead of one round trip per key.
     *
     * Redis keys come from [ScanClearRedisCache.redisKeyOf], i.e. Spring's own derivation, so they are exactly
     * the keys `put` wrote — no risk of a private copy of the prefix/conversion logic drifting and turning
     * every bulk read into a miss.
     *
     * The template and the cache serialize values with the same serializer (both come from the selected
     * store's `RedisExtProperties`), so values read here deserialize the same way `RedisCache.get` would. A
     * cached null arrives as Spring's `NullValue` sentinel and is mapped back to a null *entry* — a hit whose
     * value is null, which is what negative caching depends on.
     *
     * Falls back to per-key reads when no template is available or the cache is not one of ours, so behaviour
     * degrades to the previous cost rather than to a wrong answer.
     */
    override fun multiGet(cacheName: String, keys: Collection<Any>): Map<Any, Any?> {
        if (keys.isEmpty()) return emptyMap()
        val cache = resolveCache(cacheName) ?: return emptyMap()
        val keyList = keys.toList()
        val template = redisTemplate ?: findRedisTemplate()
        if (template == null || cache !is ScanClearRedisCache) {
            return getOneByOne(cache, keyList)
        }
        val redisKeys = keyList.map { cache.redisKeyOf(it) }
        val values = template.opsForValue().multiGet(redisKeys) ?: return emptyMap()
        val result = LinkedHashMap<Any, Any?>()
        keyList.forEachIndexed { index, key ->
            // MGET returns null in the slot of a key that does not exist — that is a miss, not a cached null.
            val raw = values.getOrNull(index) ?: return@forEachIndexed
            result[key] = if (raw is NullValue) null else raw
        }
        return result
    }

    /** Per-key fallback for [multiGet]; same cost as the pre-batch implementation. */
    private fun getOneByOne(cache: RedisCache, keys: List<Any>): Map<Any, Any?> {
        val result = LinkedHashMap<Any, Any?>()
        keys.forEach { key ->
            cache.get(key)?.let { result[key] = it.get() }
        }
        return result
    }

    private val log = LogFactory.getLog(this::class)

}
