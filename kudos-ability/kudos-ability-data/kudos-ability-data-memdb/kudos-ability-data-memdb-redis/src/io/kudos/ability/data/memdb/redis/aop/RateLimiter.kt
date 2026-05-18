package io.kudos.ability.data.memdb.redis.aop

/**
 * 方法级限流注解。被注解的方法在 [time] 秒窗口内最多允许 [count] 次调用，超出抛出
 * `ServiceException(SC_REQUEST_FREQUENTLY)`。
 *
 * 计数走 Redis（`limit.lua` 脚本里 `INCR + EXPIRE` 原子操作），见 [RateLimiterAspect]。
 *
 * 用法：
 * ```kotlin
 * @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
 * fun login(...) { ... }
 * ```
 *
 * @property time 限流时间窗口，单位秒，默认 60
 * @property count 窗口内允许的最大调用次数，默认 100
 * @property limitType 限流维度，默认 [LimitType.DEFAULT]（按方法签名）
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RateLimiter(
    /**
     * 限流时间,单位秒
     */
    val time: Int = 60,
    /**
     * 限流次数
     */
    val count: Int = 100,
    /**
     * 限流类型
     */
    val limitType: LimitType = LimitType.DEFAULT
)
