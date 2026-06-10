package io.kudos.ability.distributed.lock.redisson.locker

import io.kudos.ability.distributed.lock.common.locker.ILocker
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

/**
 * Redisson distributed lock implementation.
 * Provides acquire, lock and unlock operations backed by Redisson.
 */
class RedissonLocker : ILocker<RLock> {

    companion object {
        const val DEFAULT_WAIT_SECONDS: Long = 3
        const val DEFAULT_LEASE_SECONDS: Long = 30
    }

    @Autowired(required = false)
    private var redissonClient: RedissonClient? = null

    /**
     * Returns the injected [RedissonClient]; throws when missing rather than silently using null,
     * so callers do not later see a hard-to-trace NullPointerException.
     *
     * @return Redisson client
     * @throws IllegalArgumentException when [redissonClient] is not injected
     * @author K
     * @since 1.0.0
     */
    private fun client(): RedissonClient =
        requireNotNull(redissonClient) { "RedissonClient is not initialized; check that the redisson configuration is enabled and injected successfully" }

    /**
     * Obtain the distributed lock object.
     *
     * @param lockKey
     */
    override fun getLock(lockKey: String): RLock = client().getLock(lockKey)

    /**
     * Acquire a distributed lock.
     *
     * @param lockKey lock key
     * @return RLock
     */
    override fun lock(lockKey: String): RLock? {
        val lock = client().getLock(lockKey)
        return try {
            if (lock.tryLock(DEFAULT_WAIT_SECONDS, DEFAULT_LEASE_SECONDS, TimeUnit.SECONDS)) lock else null
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
    }

    /**
     * Acquire a distributed lock with the given lease in seconds.
     *
     * @param lockKey lockKey
     * @param timeOut timeOut
     * @return RLock
     */
    override fun lock(lockKey: String, timeOut: Long): RLock =
        client().getLock(lockKey).also { it.lock(timeOut, TimeUnit.SECONDS) }

    /**
     * Acquire a distributed lock with the given lease duration.
     *
     * @param lockKey lockKey
     * @param unit    unit
     * @param timeOut timeOut
     * @return RLock
     */
    override fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): RLock =
        client().getLock(lockKey).also { it.lock(timeOut, unit) }

    /**
     * Try to acquire the distributed lock.
     *
     * Attempts to acquire the lock within the given wait time, setting the lease on success.
     *
     * Workflow:
     * 1. Obtain the RLock instance.
     * 2. Call tryLock to attempt acquisition:
     *    - Wait up to timeOut for the lock.
     *    - On success, set the lease time to leaseTime.
     *    - On failure or timeout, return false.
     * 3. Handle interruption: if the thread is interrupted, return false.
     *
     * Parameters:
     * - timeOut: how long to wait for the lock; acquisition is retried during this period.
     * - leaseTime: lease time once acquired; the lock auto-releases after this duration.
     * - unit: time unit applied to both timeOut and leaseTime.
     *
     * Returns:
     * - true: lock acquired successfully.
     * - false: failed to acquire (timeout, interruption or other reason).
     *
     * Notes:
     * - If the thread is interrupted while waiting, InterruptedException is caught and false is returned.
     * - After acquisition, complete the business logic and release the lock within leaseTime.
     * - If business execution exceeds leaseTime the lock will auto-release, which may cause concurrency issues.
     *
     * @param lockKey lock key
     * @param unit time unit
     * @param timeOut wait time for the lock
     * @param leaseTime lease time after acquisition (lock expiry)
     * @return true if the lock was acquired, false otherwise
     */
    override fun tryLock(
        lockKey: String,
        unit: TimeUnit,
        timeOut: Long,
        leaseTime: Long
    ): Boolean =
        try {
            client().getLock(lockKey).tryLock(timeOut, leaseTime, unit)
        } catch (_: InterruptedException) {
            // Restore the interrupt flag (same as lock(lockKey)) so callers/thread pools can still observe the interruption.
            Thread.currentThread().interrupt()
            false
        }

    /**
     * Release the distributed lock.
     *
     * @param lockKey lockKey
     */
    override fun unlock(lockKey: String) {
        client().getLock(lockKey).takeIf { it.isLocked && it.isHeldByCurrentThread }?.unlock()
    }

    /**
     * Release the distributed lock.
     *
     * @param lock lockKey
     */
    override fun unlock(lock: RLock) {
        if (lock.isLocked && lock.isHeldByCurrentThread) lock.unlock()
    }
}
