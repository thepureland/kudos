package io.kudos.ability.distributed.lock.common.locker

/**
 * Distributed-lock callback SPI.
 *
 * The aspect for the `@DistributedLock` annotation (implemented in the lock-redisson module)
 * invokes the methods of this interface before and after acquiring the lock; application code
 * stores its callback instance in ThreadLocal via `DistributedLockContext.set(callback)`, and the
 * aspect retrieves and invokes it.
 *
 * @author K
 * @since 1.0.0
 */
interface IDistributedLockCallback {
    /**
     * Additional handling after a successful lock acquisition.
     * @param lockKey
     */
    fun doLockSuccess(lockKey: String) {}

    /**
     * Handling after a failed lock acquisition.
     * @param lockKey
     */
    fun doLockFail(lockKey: String)
}
