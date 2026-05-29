package io.kudos.ability.cache.remote.redis.support

import org.springframework.cache.interceptor.SimpleKey
import org.springframework.core.convert.support.GenericConversionService

/**
 * Stable `Object → String` conversion for Spring `RedisCache` keys, so that writes through one
 * access path can be reached through another.
 *
 * Without this, Spring Data Redis 4.x's default conversion service registers
 * `SimpleKey → String = SimpleKey::toString`, producing a Redis key like `"SimpleKey [k]"` when a
 * call site uses `SimpleKey("k")` but `"k"` when another call site uses the raw String. Writing
 * through one path and reading through the other then silently misses — see
 * [io.kudos.ability.cache.remote.redis.keyvalue.SimpleKeyKeyConversionBugTest] for the repro.
 *
 * Behavior:
 * - `SimpleKey` with **one** param → `String.valueOf(param)` (matches raw-String access).
 * - `SimpleKey` with **zero or multiple** params → `SimpleKey.toString()` (stable, but only
 *   reachable by constructing the same `SimpleKey` for the lookup — `@Cacheable` on multi-arg
 *   methods is mutually consistent because both write and read go through the same generator).
 * - Any other object → `String.valueOf(key)`.
 *
 * `SimpleKey.params` is private; we read it via reflection once at class load. If the field is
 * unavailable (future Spring repackage / sealing), we silently fall back to `SimpleKey.toString()`
 * for every `SimpleKey` instance — this never throws and never makes the bug worse than today.
 *
 * Ported from `org.soul.ability.cache.remote.redis.support.RedisCacheKeyConversionService`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object RedisCacheKeyConversionService {

    /** Cached reflective handle on `SimpleKey.params`; null when reflection access fails. */
    private val SIMPLE_KEY_PARAMS = runCatching {
        SimpleKey::class.java.getDeclaredField("params").apply { isAccessible = true }
    }.getOrNull()

    /**
     * Builds a [GenericConversionService] suitable for passing to
     * `RedisCacheConfiguration.withConversionService(...)`.
     *
     * Only registers `Object → String`, with SimpleKey handled inside the converter. Registering
     * a separate `SimpleKey → String` here would not work: Spring's [GenericConversionService]
     * picks the most-specific converter, and Spring's autoconfig may also register one, causing
     * ordering ambiguity. Keeping a single Object-level converter sidesteps the issue.
     */
    fun create(): GenericConversionService {
        val cs = GenericConversionService()
        cs.addConverter(Any::class.java, String::class.java) { key ->
            if (key is SimpleKey) simpleKeyToStableString(key) else key.toString()
        }
        return cs
    }

    /**
     * Returns the param's string for one-param SimpleKey (so it matches raw-String access);
     * otherwise falls back to `SimpleKey.toString()`.
     */
    private fun simpleKeyToStableString(key: SimpleKey): String {
        val params = readParams(key)
        if (params != null && params.size == 1) {
            return params[0].toString()
        }
        return key.toString()
    }

    /** Returns `SimpleKey.params` or null on reflective failure (never throws). */
    private fun readParams(key: SimpleKey): Array<*>? {
        val field = SIMPLE_KEY_PARAMS ?: return null
        return runCatching { field.get(key) as? Array<*> }.getOrNull()
    }
}
