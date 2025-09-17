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
    val leaseTime: Long = 20
)
