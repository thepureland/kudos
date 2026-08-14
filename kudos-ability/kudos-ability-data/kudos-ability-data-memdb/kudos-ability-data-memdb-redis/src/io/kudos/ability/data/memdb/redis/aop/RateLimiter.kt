package io.kudos.ability.data.memdb.redis.aop

/**
 * Method-level rate-limit annotation. The annotated method allows at most [count] calls within a [time]-second
 * window; excess calls throw `ServiceException(SC_REQUEST_FREQUENTLY)`.
 *
 * Counting goes through Redis (atomic fixed-window counter in the `limit.lua` script — the window starts at the
 * first allowed call and expires [time] seconds later); see [RateLimiterAspect]. Being a fixed window, up to
 * 2×[count] calls can pass across a window boundary; use a smaller window when that burst matters.
 *
 * Usage:
 * ```kotlin
 * @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
 * fun login(...) { ... }
 *
 * // count in a dedicated redis instance, and keep serving if that instance is down
 * @RateLimiter(time = 1, count = 50, redisName = "rateLimit", failOpen = true)
 * fun search(...) { ... }
 * ```
 *
 * @property time Rate-limit time window in seconds, default 60.
 * @property count Maximum allowed call count within the window, default 100.
 * @property limitType Rate-limit dimension, default [LimitType.DEFAULT] (by method signature).
 * @property redisName Name of the redis instance (a `kudos.ability.data.redis.redis-map` key) holding the counter;
 *                     empty means the default instance. An unknown name fails fast at call time.
 * @property failOpen Behaviour when the counter cannot be reached (Redis down, timeout, ...):
 *                    false (default) rejects the call — the limit keeps protecting the downstream;
 *                    true lets the call through — availability wins over the limit.
 *                    Only infrastructure failures are affected; exceeding the threshold always rejects.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RateLimiter(
    /**
     * Rate-limit time window, in seconds.
     */
    val time: Int = 60,
    /**
     * Rate-limit count.
     */
    val count: Int = 100,
    /**
     * Rate-limit type.
     */
    val limitType: LimitType = LimitType.DEFAULT,
    /**
     * Name of the redis instance holding the counter; empty = default instance.
     */
    val redisName: String = "",
    /**
     * Whether to allow the call through when the rate-limit infrastructure itself fails.
     */
    val failOpen: Boolean = false
)
