package io.kudos.ability.distributed.lock.common.locker

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

interface ILocker<T : Lock?> {
    fun getLock(lockKey: String): T?

    fun lock(lockKey: String): T?

    fun lock(lockKey: String, timeOut: Long): T?

    fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): T?

    fun tryLock(lockKey: String, unit: TimeUnit, timeOut: Long, leaseTime: Long): Boolean

    fun unlock(lockKey: String)

    fun unlock(lock: T)
}
