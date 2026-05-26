package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.error.ServiceException
import java.util.concurrent.locks.Lock

/**
 * Lease-based lock interface.
 *
 * Behavior: [tryLock] attempts to acquire the lock within `sec` seconds and **expires automatically once acquired**. Callers typically use [lockExecute]
 * to get automatic try-finally release; an explicit [unLock] can also release the lock early.
 *
 * Differences from [IReentrantLockProvider]:
 * - The lease lock is protected by expiration time, so **forgetting to release does not cause permanent deadlock**
 * - Reentrance is not supported (a second [tryLock] on the same key from the same thread will fail)
 * - [lockExecute] is provided only on this interface
 *
 * @author K
 * @since 1.0.0
 */
interface ILeaseLockProvider {

    /**
     * Attempts to acquire the lease lock on [lockKey] within a `sec`-second window. The lease expires automatically once elapsed.
     * @return true on success, false if the key is already held
     */
    fun tryLock(lockKey: String, sec: Int): Boolean

    /** Releases the lease lock. Not needed when lockExecute releases automatically. */
    fun unLock(key: String)

    /**
     * Executes under a lock and releases it automatically when done.
     *
     * - Lock acquired -> execute [supplier]; [unLock] in finally
     * - Lock acquisition failed + [errorCode] != null -> throws [ServiceException]
     * - Lock acquisition failed + [errorCode] == null -> returns null
     */
    fun <T> lockExecute(
        lockKey: String,
        supplier: java.util.function.Supplier<T?>,
        sec: Int,
        errorCode: IErrorCodeEnum?
    ): T? {
        if (!tryLock(lockKey, sec)) {
            errorCode?.let { throw ServiceException(it) }
            return null
        }
        return try {
            supplier.get()
        } finally {
            unLock(lockKey)
        }
    }

    /** Runnable overload with no return value. Semantics identical to the above. */
    fun lockExecute(lockKey: String, runnable: Runnable, sec: Int, errorCode: IErrorCodeEnum?) {
        if (!tryLock(lockKey, sec)) {
            errorCode?.let { throw ServiceException(it) }
            return
        }
        try {
            runnable.run()
        } finally {
            unLock(lockKey)
        }
    }
}

/**
 * Reentrant lock interface.
 *
 * Behavior: [lock] returns a [Lock] instance; the caller must **manually** [unLock]. The same thread can reenter on the same key.
 *
 * Differences from [ILeaseLockProvider]:
 * - **No timeout** — forgetting to release causes deadlock
 * - Supports reentrance
 * - Suitable for "double-checked locking" and other scenarios that require condition checks inside the lock
 *
 * @author K
 * @since 1.0.0
 */
interface IReentrantLockProvider<L : Lock> {

    /** Acquires the reentrant lock for [key]. null indicates acquisition failure (semantics determined by the implementation). */
    fun lock(key: String): L?

    /** Releases the reentrant lock. The [lock] parameter is for implementations that require the lock object (e.g. RedissonLock); local implementations can ignore it. */
    fun unLock(lock: Lock, key: String)
}

/**
 * Composite lock provider that exposes both the lease lock and the reentrant lock.
 *
 * **Historical background**: the original ILockProvider lumped both semantics into a single interface, with the KDoc noting "two mechanisms;
 * lockExecute uses only tryLock and does not route through the lock object returned by lock" — easy to misuse. After refactoring, the two
 * semantics are split into [ILeaseLockProvider] and [IReentrantLockProvider]; this interface combines them as a compatibility marker:
 * existing implementations such as [NormalLockService] / [RedissonLockProvider] continue to implement [ILockProvider], so legacy callers
 * (like LockTool / DistributedCacheGuardAspect) need no change; **new code** should prefer the specific sub-interface to express intent.
 *
 * @author K
 * @since 1.0.0
 */
interface ILockProvider<L : Lock> : ILeaseLockProvider, IReentrantLockProvider<L> {

    /** Priority ordering; smaller values come first. */
    fun order(): Int = 99

    companion object {
        const val BEAN_NAME: String = "failedLockProvider"
    }
}
