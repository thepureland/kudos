package io.kudos.ability.distributed.lock.redisson.kit

import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import io.kudos.context.kit.SpringKit
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Distributed lock utility, currently backed by Redisson.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object RedissonLockKit {

    private val lockBeans = ConcurrentHashMap<String, RedissonLocker>()

    @Volatile
    private var lockKeyPrefix: String = DEFAULT_LOCK_KEY_PREFIX

    const val DEFAULT_LOCK_KEY_PREFIX = "REDISSON::"

    /**
     * Obtain the distributed lock object.
     *
     * @param lockKey lock key
     * @return RLock
     */
    @JvmOverloads
    fun getLock(lockKey: String, lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME): RLock =
        locker(lockerBeanName).getLock(getLockKey(lockKey))

    /**
     * Acquire a distributed lock.
     *
     * @param lockKey lock key
     * @return RLock
     */
    @JvmOverloads
    fun lock(lockKey: String, lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME): RLock? =
        locker(lockerBeanName).lock(getLockKey(lockKey))

    /**
     * Acquire a distributed lock with the given lease in seconds.
     *
     * @param lockKey lockKey
     * @param timeOut timeOut
     * @return RLock
     */
    @JvmOverloads
    fun lock(lockKey: String, timeOut: Long, lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME): RLock =
        locker(lockerBeanName).lock(getLockKey(lockKey), timeOut)

    /**
     * Acquire a distributed lock with the given lease duration.
     *
     * @param lockKey lockKey
     * @param unit    unit
     * @param timeOut timeOut
     * @return RLock
     */
    @JvmOverloads
    fun lock(
        lockKey: String,
        unit: TimeUnit,
        timeOut: Long,
        lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME
    ): RLock =
        locker(lockerBeanName).lock(getLockKey(lockKey), unit, timeOut)

    /**
     * Try to acquire the lock. Returns true on success, false otherwise.
     *
     * @param lockKey   lockKey
     * @param unit      unit
     * @param timeOut   wait time for acquiring the lock
     * @param leaseTime lease time once the lock is held
     */
    @JvmOverloads
    fun tryLock(
        lockKey: String,
        unit: TimeUnit,
        timeOut: Long,
        leaseTime: Long,
        lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME
    ): Boolean =
        locker(lockerBeanName).tryLock(getLockKey(lockKey), unit, timeOut, leaseTime)

    /**
     * Release the distributed lock.
     *
     * @param lockKey lockKey
     */
    @JvmOverloads
    fun unlock(lockKey: String, lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME) {
        locker(lockerBeanName).unlock(getLockKey(lockKey))
    }

    /**
     * Release the distributed lock.
     *
     * Differs from the string-based [unlock]: releases the supplied [RLock] directly,
     * saving one key-lookup RTT, **but only when the current thread actually holds it** —
     * otherwise Redisson throws `IllegalMonitorStateException`.
     * The previous implementation only checked `isLocked`, which raised when another thread held the lock.
     * The `isHeldByCurrentThread` check is added to match [RedissonLocker.unlock].
     */
    fun unlock(lock: RLock) {
        if (lock.isLocked && lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }

    /** Bind a locker under the given bean name. Used mainly for multi-RedissonClient setups and tests. */
    fun bindLocker(locker: RedissonLocker?, lockerBeanName: String = REDISSON_LOCKER_BEAN_NAME) {
        if (locker == null) {
            lockBeans.remove(lockerBeanName)
        } else {
            lockBeans[lockerBeanName] = locker
        }
    }

    /**
     * Configure the global lock key prefix. Pass an empty string to disable the prefix.
     */
    fun setLockKeyPrefix(prefix: String) {
        lockKeyPrefix = prefix
    }

    /**
     * Returns the initialized [RedissonLocker]; fails fast when not ready
     * (indicating the Spring container is not fully built yet).
     *
     * @return the [RedissonLocker] singleton
     * @throws IllegalArgumentException when the Spring container has not injected [RedissonLocker] yet
     * @author K
     * @since 1.0.0
     */
    private fun locker(lockerBeanName: String): RedissonLocker =
        lockBeans.computeIfAbsent(lockerBeanName) {
            SpringKit.getBean(it) as RedissonLocker
        }

    /**
     * Clear cached lockers. Used mainly for tests or when the Spring container is rebuilt.
     */
    fun clearCachedLockers() {
        lockBeans.clear()
    }

    /** Bean name of the Redisson client in the Spring container. */
    const val REDISSON_CLIENT_BEAN_NAME: String = "redissonClient"

    /**
     * Fetch the [RedissonClient] bean from the Spring container.
     * Use this when callers need the native Redisson API (e.g. `RBucket` / `RBlockingQueue`).
     *
     * @return Redisson client
     * @author K
     * @since 1.0.0
     */
    fun redissonClient(): RedissonClient =
        SpringKit.getBean(REDISSON_CLIENT_BEAN_NAME) as RedissonClient

    /**
     * Concatenate the business key with [lockKeyPrefix] to form the full lock key in Redis.
     * The centralized prefix lets operators recognize redisson locks by key pattern.
     *
     * @param key business key
     * @return final key with the `REDISSON::` prefix
     * @author K
     * @since 1.0.0
     */
    fun getLockKey(key: String): String = lockKeyPrefix + key

    /** Default RedissonLocker bean name. */
    const val REDISSON_LOCKER_BEAN_NAME: String = "redissonLocker"
}
