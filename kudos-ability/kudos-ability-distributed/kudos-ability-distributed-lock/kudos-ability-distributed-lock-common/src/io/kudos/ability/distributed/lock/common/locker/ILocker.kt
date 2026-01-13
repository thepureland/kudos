package io.kudos.ability.distributed.lock.common.locker

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

/**
 * 分布式锁接口
 * 提供统一的分布式锁操作接口，支持获取锁、加锁、解锁等功能
 *
 * @param T 锁类型，继承自Lock接口
 */
interface ILocker<T : Lock?> {
    fun getLock(lockKey: String): T?

    fun lock(lockKey: String): T?

    fun lock(lockKey: String, timeOut: Long): T?

    fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): T?

    fun tryLock(lockKey: String, unit: TimeUnit, timeOut: Long, leaseTime: Long): Boolean

    fun unlock(lockKey: String)

    fun unlock(lock: T)
}
