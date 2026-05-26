package io.kudos.ability.data.memdb.redis.aop

/**
 * Method-level rate-limit annotation. The annotated method allows at most [count] calls within a [time]-second
 * window; excess calls throw `ServiceException(SC_REQUEST_FREQUENTLY)`.
 *
 * Counting goes through Redis (atomic `INCR + EXPIRE` in the `limit.lua` script); see [RateLimiterAspect].
 *
 * Usage:
 * ```kotlin
 * @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
 * fun login(...) { ... }
 * ```
 *
 * @property time Rate-limit time window in seconds, default 60.
 * @property count Maximum allowed call count within the window, default 100.
 * @property limitType Rate-limit dimension, default [LimitType.DEFAULT] (by method signature).
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
    val limitType: LimitType = LimitType.DEFAULT
)
