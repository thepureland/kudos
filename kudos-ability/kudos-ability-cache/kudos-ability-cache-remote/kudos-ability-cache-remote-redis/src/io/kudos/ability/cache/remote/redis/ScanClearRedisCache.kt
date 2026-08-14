package io.kudos.ability.cache.remote.redis

import io.kudos.context.kit.SpringKit
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.core.RedisTemplate

/**
 * Last-resort lookup of a [RedisTemplate] from the Spring context, for the `keys + delete` paths that work
 * around the Spring `cacheWriter.clean` bug.
 *
 * **Prefer the injected template.** This picks `stringRedisTemplate`, or else an arbitrary first bean, without
 * consulting `kudos.ability.cache.remoteStore`: with several redis instances configured it can land on the
 * wrong one (deleting nothing, or the wrong keys), and a template whose key serializer differs from the cache's
 * serializes the match pattern into bytes that match nothing. `RedisCacheAutoConfiguration` resolves the
 * correct template and hands it to [RedisKeyValueCacheManager], which passes it down; this function only
 * covers callers constructed without one.
 *
 * Returns null when no [RedisTemplate] bean exists in the context (rare cases such as pure unit-test mocks).
 */
@Suppress("UNCHECKED_CAST")
internal fun findRedisTemplate(): RedisTemplate<String, Any>? =
    SpringKit.getBeansOfType<RedisTemplate<*, *>>()
        .let { it["stringRedisTemplate"] ?: it.values.firstOrNull() }
        as? RedisTemplate<String, Any>

/**
 * A fix subclass of [RedisCache]: overrides [clear] to delete directly via
 * `RedisTemplate.keys(pattern) + delete(keys)`, working around the bug in Spring Boot 4.0.6's
 * built-in `RedisCache.clear()`.
 *
 * ## Background
 *
 * The built-in [RedisCache.clear] in Spring Boot 4.0.6 / Spring Data Redis 4.x ultimately calls
 * `cacheWriter.clean(name, pattern)`, but in practice under kudos's Redis config it **never deletes any key** —
 * after a full SCAN+DEL pass, all matching keys are still in Redis (verifiable with [RedisTemplate.keys]).
 * The likely cause is that Spring's internal `pattern` bytes do not match the actual stored key bytes
 * (probably different serialization paths), but there is no community fix yet.
 *
 * This bug directly caused "wipe and refill" operations such as `DomainByNameCache.reloadAll(clear = true)`
 * to leave stale values in Redis after invocation; the next `mixGet` then went local-miss -> remote-hit ->
 * backfill local, and the cache was dirty again. A whole series of integration tests
 * (SysDomainServiceTest / SysLocaleServiceTest / SysOutLineServiceTest, etc.) randomly failed when asserting
 * cache miss after batchDelete + reloadAll — all rooted in this bug.
 *
 * ## Fix
 *
 * Override [clear] to:
 * 1. Use `RedisTemplate.keys("$keyPrefix*")` to list all keys in Redis matching this cache name's prefix.
 * 2. Use `RedisTemplate.delete(keys)` to delete them in one shot.
 *
 * This is the standard "delete the keys you already know about" pattern recommended in Spring's docs;
 * behavior is predictable and follows the same path as the test infrastructure's existing `RedisTemplate`.
 *
 * Both [clear] and [invalidate] are overridden. `invalidate()` is a separate entry point on Spring's `Cache`
 * that routes to the same broken `cacheWriter` path, so overriding only `clear()` left the bug reachable by
 * anything calling `invalidate()`.
 *
 * ## Limitations
 *
 * [RedisTemplate.keys] blocks Redis on large datasets; cache key counts in this project are tiny (hundreds),
 * so this is acceptable. If cache volume grows, switch to `SCAN`-based iterative deletion (see how
 * [RedisKeyValueCacheManager.evictByPattern] does it). Note that despite this class's name, `KEYS` is what is
 * actually issued.
 *
 * @param redisTemplate the template for the redis instance holding this cache. Supplied by
 *   [RedisKeyValueCacheManager]; falls back to [findRedisTemplate] only when absent, since a statically looked
 *   up template may point at a different instance or use a different key serializer.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ScanClearRedisCache(
    name: String,
    cacheWriter: RedisCacheWriter,
    cacheConfiguration: RedisCacheConfiguration,
    private val redisTemplate: RedisTemplate<String, Any>? = null,
) : RedisCache(name, cacheWriter, cacheConfiguration) {

    override fun clear() {
        deleteAllKeys()
    }

    /**
     * Spring's `Cache.invalidate()` reports whether anything was actually removed. It reaches the same
     * `cacheWriter` path as [clear], so it needs the same workaround.
     */
    override fun invalidate(): Boolean = deleteAllKeys()

    /**
     * The full Redis key this cache would use for [key] — prefix included.
     *
     * Delegates to `RedisCache.createCacheKey`, which is `protected` and therefore reachable from this
     * subclass. Reusing Spring's own derivation matters: a hand-rolled "prefix + conversionService.convert"
     * copy would silently drift from whatever `put` actually wrote, and every bulk read would miss. (An older
     * attempt reached the equivalent method reflectively and broke under JPMS; plain inheritance has neither
     * problem.)
     *
     * @param key logical cache key
     * @return the Redis key, e.g. `"v1::user::42"`
     */
    internal fun redisKeyOf(key: Any): String = createCacheKey(key)

    /**
     * Deletes every key under this cache's prefix.
     *
     * @return true when at least one key was removed
     */
    private fun deleteAllKeys(): Boolean {
        val template = redisTemplate ?: findRedisTemplate() ?: run {
            // No RedisTemplate available (rare, e.g. pure unit-test mocks) — fall back to Spring's default logic
            return super.invalidate()
        }
        val pattern = "${cacheConfiguration.getKeyPrefixFor(name)}*"
        val matched = template.keys(pattern)
        if (matched.isEmpty()) return false
        template.delete(matched)
        return true
    }
}
