package io.kudos.context.lock

import io.kudos.base.support.KeyLockRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

/**
 * Plain lock service implementation.
 *
 * **Implementation notes**: [tryLock] (lease key + [DelayQueue] expiration) and [lock]/[unLock] ([ReentrantLock] inside [KeyLockRegistry])
 * are two separate mechanisms; the common paths in [ILockProvider.lockExecute] and [LockTool] use only [tryLock], with no linkage to the "[ReentrantLock] per key" path.
 *
 * An in-memory lock service supporting automatic expiration and cleanup.
 *
 * Core features:
 * 1. Lock expiration: uses DelayQueue for automatic lock expiration
 * 2. Daemon-thread cleanup: a background daemon thread automatically cleans up expired locks
 * 3. Reentrant locks: provides reentrant lock support via KeyLockRegistry
 * 4. Expiration: supports setting an expiration (in seconds) for each lock
 *
 * Lock storage:
 * - cacheKeyMap: maps the lock key to its expiration timestamp
 * - delayQueue: delay queue used to automatically clean up expired locks
 *
 * Expiration cleanup:
 * - The daemon thread continuously listens to delayQueue
 * - When a lock expires, it is automatically removed from cacheKeyMap
 * - Ensures expired locks do not retain memory indefinitely
 *
 * Notes:
 * - Uses a daemon thread, which does not prevent the JVM from exiting
 * - Locks are released automatically on expiration; no manual unlock required
 * - Supports concurrent access; thread-safe
 */
class NormalLockService : ILockProvider<ReentrantLock> {

    private val cacheKeyMap = ConcurrentHashMap<String, Long>()

    private val delayQueue = DelayQueue<ExpiringKey<String?>>()

    private val reentrantLockManager = KeyLockRegistry<String>()

    init {
        /**
         * Initializes a daemon thread that automatically cleans up expired locks.
         *
         * Flow:
         * 1. Create the thread and mark it as a daemon
         * 2. Continuously listen on delayQueue for expired locks
         * 3. When a lock expires, remove it from cacheKeyMap
         * 4. If interrupted, restore the interrupt status and exit
         *
         * Daemon thread characteristics:
         * - Daemon threads do not prevent JVM shutdown
         * - The JVM exits once all non-daemon threads finish
         * - Suitable for background cleanup tasks
         *
         * Blocking mechanism:
         * - delayQueue.take() blocks until an element expires
         * - Consumes no CPU; an efficient wait
         *
         * Exception handling:
         * - Catches InterruptedException and restores the interrupt status
         * - Ensures the thread responds to interruption signals correctly
         */
        // Daemon thread removes expired keys (consistent with the docs: daemon does not prevent JVM exit)
        thread(name = "kudos-normal-lock-expiry", isDaemon = true) {
            try {
                while (true) {
                    val expKey = delayQueue.take()
                    cacheKeyMap.remove(expKey.key)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * Caches a lock (sets an expiration).
     *
     * Attempts to acquire the lock and set its expiration; on success, the lock will expire automatically after the specified time.
     *
     * @param key the lock key
     * @param seconds the expiration in seconds
     */
    fun doCache(key: String, seconds: Int) {
        tryLock(key, seconds)
    }

    /**
     * Checks whether a key exists.
     *
     * Checks whether the given key exists in the lock map (i.e. whether the lock is held).
     *
     * @param key the lock key
     * @return true if the lock exists, false otherwise
     */
    fun hasKey(key: String?): Boolean = cacheKeyMap.containsKey(key)

    /**
     * Obtains the lock object.
     *
     * Non-blockingly attempts to acquire the lock object for the given key.
     *
     * @param key the lock key
     * @return the ReentrantLock object, or null if acquisition fails
     */
    override fun lock(key: String): ReentrantLock? = reentrantLockManager.tryLock(key)

    /**
     * Releases a **reentrant lock** (paired with [lock]).
     *
     * Historical bug: the old implementation `unLock(lock, key) = unLock(key)` delegated to the lease-lock release path,
     * but [lock] writes into [reentrantLockManager], not [cacheKeyMap] —
     * routing through [reentrantLockManager.unlock] is the proper paired release.
     */
    override fun unLock(lock: Lock, key: String) {
        this.reentrantLockManager.unlock(key)
    }

    /**
     * Releases a **lease lock** (paired with [tryLock]; corresponds to the finally release path of [lockExecute]).
     *
     * Historical bug: the old implementation `unLock(key) = reentrantLockManager.unlock(key)`, but [tryLock]
     * writes into [cacheKeyMap] — `reentrantLockManager` has no entry at all, so it always threw
     * "No lock found". This caused [lockExecute] to be broken since its creation.
     *
     * Now fixed to:
     * 1. Remove the key from [cacheKeyMap]
     * 2. Explicitly remove the corresponding entry from [delayQueue] to prevent the daemon thread from later mis-cleaning a new lock that
     *    "reuses the same key" (relies on [ExpiringKey.equals] comparing only the key, not expireAtMillis)
     */
    override fun unLock(key: String) {
        cacheKeyMap.remove(key)?.let { previousExpire ->
            // When the daemon thread later processes the old entry it would remove cacheKeyMap[key] again;
            // if the key has been reused by a new lock of the same name by then, the new lock would be wrongly cleared — so remove it proactively
            delayQueue.remove(ExpiringKey(key, previousExpire))
        }
    }

    /**
     * Tries to acquire the lock (with expiration).
     *
     * Attempts to acquire the lock for the given key and, on success, sets its expiration.
     *
     * Flow:
     * 1. Check whether the lock exists: if the key already exists, the lock is held, return false
     * 2. Compute the expiration: current time + expiration seconds
     * 3. Atomic put: use putIfAbsent to atomically insert the key and expiration
     * 4. Decide the result:
     *    - If old is null, this thread was the first; insertion succeeded; add to the delay queue; return true
     *    - If old is non-null, another thread inserted first; return false
     *
     * Concurrency safety:
     * - Uses ConcurrentHashMap for thread safety
     * - putIfAbsent is atomic, ensuring only one thread can insert successfully
     * - The first successful thread is responsible for adding the key to the delay queue
     *
     * Expiration mechanism:
     * - The lock expiration is stored in cacheKeyMap
     * - Once the expiration time is reached, the daemon thread cleans it up automatically
     * - After expiration, other threads can acquire the lock again
     *
     * Return value:
     * - true: lock acquired successfully
     * - false: lock is already held by another thread
     *
     * Notes:
     * - Locks expire automatically after the specified time; no manual release required
     * - Expired locks can be reacquired
     * - putIfAbsent ensures concurrency safety
     *
     * @param lockKey the lock key
     * @param sec the lock expiration in seconds
     * @return true if the lock was acquired, false if the lock is already held
     */
    override fun tryLock(lockKey: String, sec: Int): Boolean {
        val expireTime = System.currentTimeMillis() + (sec * 1000)
        // The old implementation called containsKey first then putIfAbsent — the prior check was redundant:
        // putIfAbsent already atomically performs "check + insert" and signals success via its return value.
        // Removing the redundant check makes the logic tighter and saves one map access under concurrency.
        val old = cacheKeyMap.putIfAbsent(lockKey, expireTime)
        return if (old == null) {
            // This thread was the first successful inserter; add to the delay queue for the daemon thread to clean up on expiration
            delayQueue.put(ExpiringKey(lockKey, expireTime))
            true
        } else {
            false
        }
    }

    /**
     * Wrapper class for an expiring key.
     *
     * Implements Delayed; used by DelayQueue to store keys with expiration.
     *
     * Core features:
     * 1. Stores the key and expiration: holds the key object and the expiration timestamp
     * 2. Computes remaining time: implements getDelay to compute time until expiration
     * 3. Comparison/sorting: implements compareTo for DelayQueue ordering
     * 4. Equality: equality is determined solely by key (used by remove)
     *
     * Expiration:
     * - Uses an absolute timestamp in milliseconds
     * - Formula: System.currentTimeMillis() + delayMillis
     * - Stored in the expireAtMillis field
     *
     * DelayQueue usage:
     * - DelayQueue orders by the value returned from getDelay
     * - Earlier-expiring elements come first
     * - take() blocks until an element has expired
     *
     * Equality:
     * - equals and hashCode are based solely on key
     * - Allows multiple ExpiringKey instances for the same key (with different expirations)
     * - remove can match by key
     *
     * @param K the key type
     * @param key the key object
     * @param expireAtMillis the absolute expiration time in milliseconds
     */
    private class ExpiringKey<K>(
        val key: K?,
        /** Absolute expiration time in milliseconds (System.currentTimeMillis() + delayMillis). */
        private val expireAtMillis: Long
    ) : Delayed {

        /**
         * Computes remaining time until expiration.
         *
         * Calculates the difference between the expiration time and the current time, converted into the specified time unit.
         *
         * Calculation:
         * - remaining millis = expiration timestamp - current timestamp
         * - Convert millis to the requested time unit
         * - Returns negative or 0 if already expired
         *
         * Time-unit conversion:
         * - Uses TimeUnit.convert
         * - Supports all units defined by TimeUnit
         *
         * @param unit the time unit
         * @return remaining time until expiration in the given unit; <= 0 if already expired
         */
        override fun getDelay(unit: TimeUnit): Long {
            val remainingMillis = expireAtMillis - System.currentTimeMillis()
            // Convert the millisecond difference into the unit requested by the caller
            return unit.convert(remainingMillis, TimeUnit.MILLISECONDS)
        }

        /**
         * Compares two Delayed objects by expiration time.
         *
         * Used by DelayQueue for ordering; earlier-expiring elements come first.
         *
         * Comparison:
         * - Both sides are converted to milliseconds for comparison
         * - Compute the difference in remaining time
         * - Returns the sign of the difference (-1, 0, 1)
         *
         * Ordering:
         * - Earlier-expiring elements are placed in front
         * - Elements with the same expiration are unordered with respect to each other
         * - DelayQueue serves earlier-expiring elements first
         *
         * @param other the other Delayed object
         * @return negative if this expires earlier; 0 if equal; positive if this expires later
         */
        override fun compareTo(other: Delayed): Int =
            getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))

        /**
         * Equality for two ExpiringKeys.
         *
         * Equality is determined solely by key, ignoring expiration.
         *
         * Rules:
         * - Same object reference: true
         * - Different type: false
         * - Equal keys: true
         *
         * Rationale:
         * - Allows multiple ExpiringKey instances for the same key
         * - remove can match by key
         * - Simplifies expiring-key management
         *
         * @param other the other object
         * @return true if equal, false otherwise
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExpiringKey<*>) return false
            // Compare by key only so that remove can find matches
            return key == other.key
        }

        /**
         * Computes the hash code.
         *
         * The hash code is based solely on key, consistent with equals.
         *
         * @return the key's hash code
         */
        override fun hashCode(): Int = key.hashCode()
    }

}
