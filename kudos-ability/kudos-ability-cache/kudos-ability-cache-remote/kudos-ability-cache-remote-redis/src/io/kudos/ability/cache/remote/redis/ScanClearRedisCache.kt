package io.kudos.ability.cache.remote.redis

import io.kudos.context.kit.SpringKit
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.core.RedisTemplate

/**
 * Grabs an available [RedisTemplate] from the Spring context, shared by [ScanClearRedisCache] and
 * [RedisKeyValueCacheManager.evictByPattern] — both need to bypass the Spring `cacheWriter.clear`
 * bug that fails to delete keys, and instead use `keys + delete` directly. Prefers `stringRedisTemplate`,
 * falls back to any other.
 *
 * Returns null only when no [RedisTemplate] bean exists in the context (rare cases such as pure unit-test mocks).
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
 * `RedisTemplate` is resolved lazily via [SpringKit] (looked up every clear; cost is negligible since clear
 * is a low-frequency operation).
 *
 * ## Limitations
 *
 * [RedisTemplate.keys] blocks Redis on large datasets; cache key counts in this project are tiny (hundreds),
 * so this is acceptable. If cache volume grows, switch to `SCAN`-based iterative deletion (see how
 * [RedisKeyValueCacheManager.evictByPattern] does it).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ScanClearRedisCache(
    name: String,
    cacheWriter: RedisCacheWriter,
    cacheConfiguration: RedisCacheConfiguration,
) : RedisCache(name, cacheWriter, cacheConfiguration) {

    override fun clear() {
        val template = findRedisTemplate() ?: run {
            // No RedisTemplate in the context (rare, e.g. pure unit-test mocks) — fall back to Spring's default logic
            super.clear()
            return
        }
        val pattern = "${cacheConfiguration.getKeyPrefixFor(name)}*"
        val matched = template.keys(pattern)
        if (matched.isNotEmpty()) {
            template.delete(matched)
        }
    }
}
