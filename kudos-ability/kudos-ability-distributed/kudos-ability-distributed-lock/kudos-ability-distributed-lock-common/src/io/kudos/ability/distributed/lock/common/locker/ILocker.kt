package io.kudos.ability.distributed.lock.common.locker

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

/**
 * Distributed lock SPI. See `kudos-ability-distributed-lock-redisson` for the concrete implementation.
 *
 * Design intent: let business code `@Autowired ILocker<*>` and call `lock(key, ...)` / `unlock(key)`
 * without touching specific clients such as Redisson / Curator / Etcd.
 *
 * @param T lock type, extending the standard [Lock] — concrete implementations typically return an `RLock` (Redisson) or similar reentrant lock object
 * @author K
 * @since 1.0.0
 */
interface ILocker<T : Lock?> {
    /** Return only the lock object reference (does not acquire). Returns null if the lock cannot be constructed. */
    fun getLock(lockKey: String): T?

    /**
     * Convenience signature for acquiring a lock.
     *
     * Implementations must not block forever; when the lock cannot be acquired within the default
     * wait window they should return null. Code that needs explicit wait time / lease should
     * prefer [tryLock].
     */
    fun lock(lockKey: String): T?

    /** Blocking acquisition with a timeout in seconds. */
    fun lock(lockKey: String, timeOut: Long): T?

    /** Blocking acquisition with a custom time unit and timeout. */
    fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): T?

    /**
     * Non-blocking acquisition attempt with a lease time — the lock is auto-released after the
     * lease expires, preventing forever-held locks if the holding thread crashes.
     *
     * @return true on success; false if the lock was not acquired within timeOut
     */
    fun tryLock(lockKey: String, unit: TimeUnit, timeOut: Long, leaseTime: Long): Boolean

    /** Release the lock by key — the caller is responsible for ensuring it holds the lock being released. */
    fun unlock(lockKey: String)

    /** Release the lock object directly — avoids the extra by-key lookup in [unlock]. */
    fun unlock(lock: T)
}
