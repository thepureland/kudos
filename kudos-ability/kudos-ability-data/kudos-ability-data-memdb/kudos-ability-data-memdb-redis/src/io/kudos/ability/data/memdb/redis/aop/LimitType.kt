package io.kudos.ability.data.memdb.redis.aop

enum class LimitType {
    /**
     * 默认策略
     */
    DEFAULT,

    /**
     * 根據用戶進行限流
     */
    USER,

    /**
     * 根据IP进行限流
     */
    IP
}
