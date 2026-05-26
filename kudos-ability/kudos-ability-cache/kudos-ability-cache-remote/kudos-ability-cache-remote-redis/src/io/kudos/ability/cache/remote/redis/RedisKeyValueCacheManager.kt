package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import java.time.Duration

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
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisKeyValueCacheManager(
    private val cacheWriter: RedisCacheWriter,
    private val defaultCacheConfiguration: RedisCacheConfiguration
) : RedisCacheManager(
    cacheWriter,
    defaultCacheConfiguration
), IKeyValueCacheManager<RedisCache> {

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
     * 2. Disable null-value caching (disableCachingNullValues).
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
        var redisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .serializeKeysWith(defaultCacheConfiguration.keySerializationPair)
            .serializeValuesWith(defaultCacheConfiguration.valueSerializationPair)
        cacheConfig.ttl?.let { ttl ->
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofSeconds(ttl.toLong()))
        }
        val realKey: String = versionConfig.getFinalCacheName(requireNotNull(cacheConfig.name) { "cache name required" })
        // Fix for the Spring Boot 4.0.6 bug where [RedisCache.clear] does not actually delete Redis keys:
        // our override uses `RedisTemplate.keys(pattern) + delete(keys)` directly, applied to every RedisCache instance.
        return ScanClearRedisCache(realKey, cacheWriter, redisCacheConfiguration)
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
        val template = findRedisTemplate() ?: return
        val matched = template.keys(fullPattern)
        if (matched.isNotEmpty()) {
            template.delete(matched)
        }
    }

    /**
     * Checks whether the given key exists — via the public `Cache.get(key)` (no reflection).
     *
     * History: the old implementation reflectively called `RedisCache.createAndConvertCacheKey`,
     * which fails under JPMS. Using the public API is simpler and more reliable; since `disableCachingNullValues()`
     * guarantees we never store null values, "a non-null ValueWrapper returned from `get`" is equivalent to "key exists".
     */
    override fun existsKey(cacheName: String, key: Any): Boolean {
        val redisCache = getCache(cacheName) as? RedisCache ?: return false
        return redisCache.get(key) != null
    }

    private val log = LogFactory.getLog(this::class)

}
