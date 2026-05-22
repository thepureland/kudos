package io.kudos.ability.data.memdb.redis.aop

/**
 * [RateLimiter] 的限流维度枚举。值决定 Redis 计数 key 的拼装方式（见
 * `RateLimiterAspect.getCombineKey`）。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class LimitType {
    /**
     * 默认策略：按"方法签名"限流，整个进程内所有调用方共享同一计数。
     */
    DEFAULT,

    /**
     * 按用户限流：每个 `user.id` 各自计数。要求 `KudosContextHolder.get().user.id` 非空。
     */
    USER,

    /**
     * 按 IP 限流：每个客户端 IP 各自计数。要求 `KudosContextHolder.get().clientInfo.ip` 非空。
     */
    IP
}
