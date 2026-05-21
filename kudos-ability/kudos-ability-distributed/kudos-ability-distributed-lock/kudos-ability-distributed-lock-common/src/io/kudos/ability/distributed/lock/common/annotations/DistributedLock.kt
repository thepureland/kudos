package io.kudos.ability.distributed.lock.common.annotations

import java.lang.annotation.Inherited

/**
 * 分布式方法锁
 *
 * @author K
 * @author hanson
 * @since 1.0.0
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
annotation class DistributedLock(
    /**
     * 分布式锁的key
     */
    val key: String = "",
    /**
     * 分布式锁等待时间
     */
    val waitTime: Long = 0,
    /**
     * 分布式锁的租期：失效时间
     */
    val leaseTime: Long = 20,
    /**
     * 拿不到锁时是否抛出异常。
     *
     * 默认抛出 [io.kudos.ability.distributed.lock.common.exception.DistributedLockAcquireException]，
     * 避免非 nullable 返回值业务方法在调用方因 null 产生延迟 NPE。需要兼容旧行为时可显式设为 false。
     */
    val throwOnFailure: Boolean = true,
    /**
     * 使用指定名称的 RedissonLocker bean。为空时使用默认 locker。
     *
     * 多 Redis / 多 RedissonClient 场景可自行声明多个 RedissonLocker bean，并在注解上指定 bean 名。
     */
    val lockerBeanName: String = ""
)
