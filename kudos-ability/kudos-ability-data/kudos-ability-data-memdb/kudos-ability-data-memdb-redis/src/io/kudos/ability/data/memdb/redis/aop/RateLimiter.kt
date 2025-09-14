package io.kudos.ability.data.memdb.redis.aop

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
