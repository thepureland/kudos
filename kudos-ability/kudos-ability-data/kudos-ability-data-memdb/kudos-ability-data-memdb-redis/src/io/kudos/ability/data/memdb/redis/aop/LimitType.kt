package io.kudos.ability.data.memdb.redis.aop

/**
 * Rate-limit dimension enum for [RateLimiter]. The value determines how the Redis counter key is assembled
 * (see `RateLimiterAspect.getCombineKey`).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class LimitType {
    /**
     * Default strategy: rate-limit by "method signature"; all callers within the process share the same counter.
     */
    DEFAULT,

    /**
     * Rate-limit by user: each `user.id` has its own counter. Requires `KudosContextHolder.get().user.id` to be non-null.
     */
    USER,

    /**
     * Rate-limit by IP: each client IP has its own counter. Requires `KudosContextHolder.get().clientInfo.ip` to be non-null.
     */
    IP
}
