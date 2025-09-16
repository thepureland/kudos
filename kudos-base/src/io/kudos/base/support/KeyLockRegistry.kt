package io.kudos.base.support

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 一个基于 Key 的锁注册表。
 *
 * 特点：
 * - 不同的 key 拥有独立的 ReentrantLock，互不影响；
 * - 相同的 key 会复用同一把锁，从而保证串行化访问；
 * - 内部维护引用计数（usageCount），在没有线程使用时会移除锁，避免内存泄漏；
 * - 提供阻塞、非阻塞、超时三种加锁模式。
 *
 * 典型使用场景：
 * - 替代 synchronized(key) 的写法；
 * - 控制对某一类资源（如订单号、用户ID）的并发访问。
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
class KeyLockRegistry<K : Any> {

    /**
     * 封装锁对象和使用计数。
     *
     * @property lock 实际的 ReentrantLock
     * @property usageCount 当前锁被引用的次数
     */
    private class LockWrapper {
        val lock = ReentrantLock()
        val usageCount = AtomicInteger(0)
    }

    /** 存放 key -> LockWrapper 的映射表 */
    private val lockMap = ConcurrentHashMap<K, LockWrapper>()

    /**
     * 获取指定 key 的锁，并在锁保护的临界区中执行 block。
     * 执行完成后会自动解锁。
     *
     * @param key 锁的标识
     * @param block 临界区逻辑
     * @return block 的返回值
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
     * 尝试非阻塞地获取 key 对应的锁。
     *
     * @param key 锁的标识
     * @return null表示锁已被其他线程持有
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
     * 尝试在指定超时时间内获取 key 对应的锁。
     *
     * @param key 锁的标识
     * @param timeout 等待时长
     * @param unit 时间单位
     * @return null表示超时
     * @throws InterruptedException 如果等待过程中被中断
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
     * 手动解锁（适用于 tryLock 成功后未使用 withLock 的情况）。
     *
     * @param key 锁的标识
     * @throws IllegalStateException 如果 key 对应的锁不存在
     */
    fun unlock(key: K) {
        val wrapper = lockMap[key] ?: error("No lock found for key=$key")
        if (wrapper.lock.isHeldByCurrentThread) {
            wrapper.lock.unlock()
        }
        releaseWrapper(key, wrapper)
    }

    /**
     * 获取当前活跃的 key 数量（即仍然存在于 lockMap 中的锁数量）。
     *
     * @return 活跃锁的数量
     */
    fun getActiveLockCount(): Int = lockMap.size

    /**
     * 释放锁包装对象：
     * - usageCount 减 1；
     * - 如果减到 0，则从 lockMap 中移除该锁。
     *
     * @param key 锁的标识
     * @param wrapper 对应的锁包装对象
     */
    private fun releaseWrapper(key: K, wrapper: LockWrapper) {
        if (wrapper.usageCount.decrementAndGet() <= 0) {
            lockMap.remove(key, wrapper)
        }
    }

}
