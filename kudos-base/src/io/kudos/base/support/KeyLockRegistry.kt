package io.kudos.base.support

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A key-based lock registry.
 *
 * Features:
 * - Different keys have independent ReentrantLocks that do not affect each other;
 * - The same key reuses the same lock, ensuring serialized access;
 * - Maintains a reference count (usageCount) internally and removes the lock when no thread is using it, avoiding memory leaks;
 * - Provides blocking, non-blocking, and timeout-based locking modes.
 *
 * Typical use cases:
 * - As a replacement for synchronized(key);
 * - To control concurrent access to a class of resources (such as order numbers or user IDs).
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
class KeyLockRegistry<K : Any> {

    /**
     * Wraps the lock object and its usage count.
     *
     * @property lock The actual ReentrantLock
     * @property usageCount Current reference count of the lock
     */
    private class LockWrapper {
        val lock = ReentrantLock()
        val usageCount = AtomicInteger(0)
    }

    /** Mapping table that stores key -> LockWrapper */
    private val lockMap = ConcurrentHashMap<K, LockWrapper>()

    /**
     * Acquires the lock for the specified key and executes the given code block within the critical section protected by the lock.
     *
     * Automatically manages lock acquisition and release, ensuring the lock is always released after the code block completes.
     *
     * Workflow:
     * 1. Get or create the lock wrapper: use computeIfAbsent to thread-safely obtain or create the lock
     * 2. Increment reference count: usageCount plus 1, indicating a thread is using this lock
     * 3. Acquire the lock and execute: use withLock to acquire the lock and execute the code block (blocking wait)
     * 4. Release the reference count: call releaseWrapper in the finally block to decrement the count
     *
     * Lock management:
     * - Uses try-finally to ensure the reference count is always released
     * - When the reference count drops to 0, the lock is removed from lockMap to avoid memory leaks
     * - Supports reentrant locks; the same thread can acquire the same lock multiple times
     *
     * Use cases:
     * - When you need to ensure the code block runs under lock protection
     * - When you want automatic lock acquisition and release
     * - Suitable for scenarios that require blocking-wait locks
     *
     * Notes:
     * - If the lock is held by another thread, the current thread will block and wait
     * - The lock is automatically released after the code block completes
     * - Even if the code block throws an exception, the lock will still be released properly
     *
     * @param key The lock identifier
     * @param block The code block to execute under lock protection
     * @return The return value of the code block
     */
    fun <T> withLock(key: K, block: () -> T): T {
        val wrapper = lockMap.computeIfAbsent(key) { LockWrapper() }
        wrapper.usageCount.incrementAndGet()
        try {
            return wrapper.lock.withLock(block)
        } finally {
            releaseWrapper(key, wrapper)
        }
    }

    /**
     * Attempts to acquire the lock for the key in a non-blocking manner.
     *
     * Immediately attempts to acquire the lock; if the lock is already held by another thread, returns null immediately without waiting.
     *
     * Workflow:
     * 1. Get or create the lock wrapper: use computeIfAbsent to ensure thread safety
     * 2. Increment reference count: usageCount plus 1
     * 3. Try to acquire the lock: call tryLock() in non-blocking mode
     * 4. Failure handling: if acquisition fails, immediately release the reference count and return null
     * 5. Success: return the lock object; the caller must release the lock manually
     *
     * Return value:
     * - Non-null: lock acquired successfully, returns the ReentrantLock object; unlock() must be called manually
     * - null: lock is already held by another thread, acquisition failed
     *
     * Use cases:
     * - When non-blocking lock acquisition is needed
     * - When alternative logic must run on failure
     * - When you do not want to block the current thread
     *
     * Notes:
     * - After successful acquisition, you must call unlock manually to release the lock
     * - If acquisition fails, the reference count is automatically released; no additional handling is required
     * - Suitable for fail-fast scenarios
     *
     * @param key The lock identifier
     * @return A ReentrantLock object on success, or null on failure
     */
    fun tryLock(key: K): ReentrantLock? {
        val wrapper = lockMap.computeIfAbsent(key) { LockWrapper() }
        wrapper.usageCount.incrementAndGet()
        val acquired = wrapper.lock.tryLock()
        if (!acquired) {
            releaseWrapper(key, wrapper)
            return null
        }
        return wrapper.lock
    }

    /**
     * Attempts to acquire the lock for the key within the specified timeout.
     *
     * Tries to acquire the lock within the given time; returns null if the timeout elapses without acquiring it.
     *
     * Workflow:
     * 1. Get or create the lock wrapper: use computeIfAbsent to ensure thread safety
     * 2. Increment reference count: usageCount plus 1
     * 3. Try to acquire the lock: call tryLock(timeout, unit) to wait for the specified time
     * 4. Timeout handling: if the timeout elapses without acquiring the lock, release the reference count and return null
     * 5. Success: return the lock object; the caller must release the lock manually
     *
     * Timeout mechanism:
     * - Continues to attempt acquisition within the timeout window
     * - Returns the lock object immediately once acquired
     * - Returns null if the timeout elapses without acquiring the lock
     *
     * Interrupt handling:
     * - If the thread is interrupted while waiting, an InterruptedException is thrown
     * - The caller must handle the interruption exception
     *
     * Use cases:
     * - When waiting for the lock within a bounded period
     * - When you do not want to wait indefinitely
     * - When alternative logic must run after a timeout
     *
     * Notes:
     * - After successful acquisition, you must call unlock manually to release the lock
     * - If acquisition fails or times out, the reference count is automatically released
     * - InterruptedException must be handled
     *
     * @param key The lock identifier
     * @param timeout The wait duration
     * @param unit The time unit
     * @return A ReentrantLock object on success, or null on timeout or failure
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    @Throws(InterruptedException::class)
    fun tryLock(key: K, timeout: Long, unit: TimeUnit): ReentrantLock? {
        val wrapper = lockMap.computeIfAbsent(key) { LockWrapper() }
        wrapper.usageCount.incrementAndGet()
        val acquired = wrapper.lock.tryLock(timeout, unit)
        if (!acquired) {
            releaseWrapper(key, wrapper)
            return null
        }
        return wrapper.lock
    }

    /**
     * Manually unlocks.
     *
     * Releases the lock acquired via tryLock and decrements the reference count.
     *
     * Workflow:
     * 1. Find the lock wrapper: look up the lock for the given key in lockMap
     * 2. Check the lock holder: only unlock if the current thread holds the lock
     * 3. Release the lock: call lock.unlock() to release the lock
     * 4. Release the reference count: call releaseWrapper to decrement the count
     *
     * Use cases:
     * - Used together with tryLock to manually release the lock
     * - When the lock must be released outside its protected scope
     *
     * Notes:
     * - The unlock operation runs only when the current thread holds the lock
     * - If no lock exists for the key, an exception is thrown
     * - After unlocking, the reference count is decremented; if it drops to 0, the lock is removed
     *
     * @param key The lock identifier
     * @throws IllegalStateException If no lock exists for the key
     */
    fun unlock(key: K) {
        val wrapper = lockMap[key] ?: error("No lock found for key=$key")
        if (wrapper.lock.isHeldByCurrentThread) {
            wrapper.lock.unlock()
        }
        releaseWrapper(key, wrapper)
    }

    /**
     * Returns the number of currently active locks.
     *
     * Returns the number of locks currently present in lockMap; used for monitoring and debugging.
     *
     * Return value:
     * - Returns the size of lockMap, i.e., how many different keys currently hold locks
     * - 0 means there are no active locks at the moment
     *
     * Use cases:
     * - Monitoring lock usage
     * - Debugging lock leaks
     * - Performance analysis
     *
     * @return The number of currently active locks
     */
    fun getActiveLockCount(): Int = lockMap.size

    /**
     * Releases the lock wrapper.
     *
     * Decrements the lock's reference count and removes the lock from lockMap if the count drops to 0.
     *
     * Workflow:
     * 1. Decrement the reference count: usageCount minus 1 (atomic operation)
     * 2. Check the reference count: if it drops to 0 or below, no thread is using the lock
     * 3. Remove the lock: remove the mapping from key to wrapper in lockMap
     *
     * Memory management:
     * - Uses a reference counting mechanism to avoid memory leaks
     * - The lock is removed only after all threads have released it
     * - Uses remove(key, wrapper) to ensure only the specified wrapper is removed, avoiding concurrency issues
     *
     * Thread safety:
     * - usageCount uses AtomicInteger to ensure atomic operations
     * - lockMap is a ConcurrentHashMap that supports concurrent operations
     * - The remove operation uses a CAS mechanism to ensure thread safety
     *
     * Notes:
     * - The lock is removed only when the reference count drops to 0, preventing removal of in-use locks
     * - Uses remove(key, wrapper) rather than remove(key) to ensure only the specified wrapper instance is removed
     *
     * @param key The lock identifier
     * @param wrapper The corresponding lock wrapper
     */
    private fun releaseWrapper(key: K, wrapper: LockWrapper) {
        if (wrapper.usageCount.decrementAndGet() <= 0) {
            lockMap.remove(key, wrapper)
        }
    }

}
