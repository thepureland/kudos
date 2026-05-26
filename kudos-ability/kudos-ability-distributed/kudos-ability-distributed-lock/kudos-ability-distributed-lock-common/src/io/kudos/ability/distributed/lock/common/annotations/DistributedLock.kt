package io.kudos.ability.distributed.lock.common.annotations

import java.lang.annotation.Inherited

/**
 * Distributed method lock.
 *
 * @author K
 * @author hanson
 * @author AI: Codex
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
     * Distributed lock key.
     */
    val key: String = "",
    /**
     * Wait time for the distributed lock.
     */
    val waitTime: Long = 0,
    /**
     * Lease time for the distributed lock: the expiration interval.
     */
    val leaseTime: Long = 20,
    /**
     * Whether to throw an exception when the lock cannot be acquired.
     *
     * Defaults to throwing [io.kudos.ability.distributed.lock.common.exception.DistributedLockAcquireException]
     * to prevent non-nullable business methods from causing a delayed NPE in the caller. Set to
     * false explicitly when legacy behaviour is needed.
     */
    val throwOnFailure: Boolean = true,
    /**
     * Name of the RedissonLocker bean to use. When empty, the default locker is used.
     *
     * For multi-Redis / multi-RedissonClient setups, declare additional RedissonLocker beans and
     * reference the bean name on the annotation.
     */
    val lockerBeanName: String = ""
)
