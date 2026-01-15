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
 * @author AI: ChatGPT
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
     * 获取指定key的锁，并在锁保护的临界区中执行代码块
     * 
     * 自动管理锁的获取和释放，确保代码块执行完成后锁一定会被释放。
     * 
     * 工作流程：
     * 1. 获取或创建锁包装对象：使用computeIfAbsent确保线程安全地获取或创建锁
     * 2. 增加引用计数：usageCount加1，表示有线程正在使用该锁
     * 3. 获取锁并执行：使用withLock获取锁并执行代码块（阻塞等待）
     * 4. 释放引用计数：在finally块中调用releaseWrapper减少引用计数
     * 
     * 锁管理：
     * - 使用try-finally确保引用计数一定会被释放
     * - 如果引用计数减到0，锁会被从lockMap中移除，避免内存泄漏
     * - 支持可重入锁，同一线程可以多次获取同一把锁
     * 
     * 使用场景：
     * - 需要确保代码块在锁保护下执行
     * - 希望自动管理锁的获取和释放
     * - 适合需要阻塞等待锁的场景
     * 
     * 注意事项：
     * - 如果锁被其他线程持有，当前线程会阻塞等待
     * - 代码块执行完成后锁会自动释放
     * - 即使代码块抛出异常，锁也会被正确释放
     * 
     * @param key 锁的标识
     * @param block 需要在锁保护下执行的代码块
     * @return 代码块的返回值
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
     * 尝试非阻塞地获取key对应的锁
     * 
     * 立即尝试获取锁，如果锁已被其他线程持有，立即返回null而不等待。
     * 
     * 工作流程：
     * 1. 获取或创建锁包装对象：使用computeIfAbsent确保线程安全
     * 2. 增加引用计数：usageCount加1
     * 3. 尝试获取锁：调用tryLock()，非阻塞方式
     * 4. 获取失败处理：如果获取失败，立即释放引用计数并返回null
     * 5. 获取成功：返回锁对象，调用方需要手动释放锁
     * 
     * 返回值：
     * - 非null：成功获取锁，返回ReentrantLock对象，需要手动调用unlock释放
     * - null：锁已被其他线程持有，获取失败
     * 
     * 使用场景：
     * - 需要非阻塞的锁获取
     * - 获取失败时需要执行其他逻辑
     * - 不希望阻塞当前线程
     * 
     * 注意事项：
     * - 获取成功后必须手动调用unlock释放锁
     * - 如果获取失败，引用计数会自动释放，无需额外处理
     * - 适合需要快速失败（fail-fast）的场景
     * 
     * @param key 锁的标识
     * @return ReentrantLock对象表示获取成功，null表示获取失败
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
     * 尝试在指定超时时间内获取key对应的锁
     * 
     * 在指定时间内尝试获取锁，如果超时仍未获取到，返回null。
     * 
     * 工作流程：
     * 1. 获取或创建锁包装对象：使用computeIfAbsent确保线程安全
     * 2. 增加引用计数：usageCount加1
     * 3. 尝试获取锁：调用tryLock(timeout, unit)，在指定时间内等待
     * 4. 超时处理：如果超时仍未获取到锁，释放引用计数并返回null
     * 5. 获取成功：返回锁对象，调用方需要手动释放锁
     * 
     * 超时机制：
     * - 在timeout时间内会持续尝试获取锁
     * - 如果timeout时间内获取到锁，立即返回锁对象
     * - 如果超时仍未获取到，返回null
     * 
     * 中断处理：
     * - 如果等待过程中线程被中断，会抛出InterruptedException
     * - 调用方需要处理中断异常
     * 
     * 使用场景：
     * - 需要在一定时间内等待锁
     * - 不希望无限期等待
     * - 需要在超时后执行其他逻辑
     * 
     * 注意事项：
     * - 获取成功后必须手动调用unlock释放锁
     * - 如果获取失败或超时，引用计数会自动释放
     * - 需要处理InterruptedException异常
     * 
     * @param key 锁的标识
     * @param timeout 等待时长
     * @param unit 时间单位
     * @return ReentrantLock对象表示获取成功，null表示超时或获取失败
     * @throws InterruptedException 如果等待过程中线程被中断
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
     * 手动解锁
     * 
     * 释放通过tryLock获取的锁，并减少引用计数。
     * 
     * 工作流程：
     * 1. 查找锁包装对象：从lockMap中查找key对应的锁
     * 2. 检查锁持有者：只有当前线程持有锁时才执行解锁
     * 3. 释放锁：调用lock.unlock()释放锁
     * 4. 释放引用计数：调用releaseWrapper减少引用计数
     * 
     * 使用场景：
     * - 配合tryLock使用，手动释放锁
     * - 需要在锁保护范围外释放锁
     * 
     * 注意事项：
     * - 只有当前线程持有锁时才会执行解锁操作
     * - 如果key对应的锁不存在，会抛出异常
     * - 解锁后引用计数会减少，如果减到0，锁会被移除
     * 
     * @param key 锁的标识
     * @throws IllegalStateException 如果key对应的锁不存在
     */
    fun unlock(key: K) {
        val wrapper = lockMap[key] ?: error("No lock found for key=$key")
        if (wrapper.lock.isHeldByCurrentThread) {
            wrapper.lock.unlock()
        }
        releaseWrapper(key, wrapper)
    }

    /**
     * 获取当前活跃的锁数量
     * 
     * 返回当前lockMap中存在的锁数量，用于监控和调试。
     * 
     * 返回值说明：
     * - 返回lockMap的大小，即当前有多少个不同的key拥有锁
     * - 如果返回0，表示当前没有任何活跃的锁
     * 
     * 使用场景：
     * - 监控锁的使用情况
     * - 调试锁泄漏问题
     * - 性能分析
     * 
     * @return 当前活跃锁的数量
     */
    fun getActiveLockCount(): Int = lockMap.size

    /**
     * 释放锁包装对象
     * 
     * 减少锁的引用计数，如果引用计数减到0，从lockMap中移除该锁。
     * 
     * 工作流程：
     * 1. 减少引用计数：usageCount减1（原子操作）
     * 2. 检查引用计数：如果减到0或以下，说明没有线程在使用该锁
     * 3. 移除锁：从lockMap中移除该key和wrapper的映射
     * 
     * 内存管理：
     * - 使用引用计数机制避免内存泄漏
     * - 只有当所有线程都释放锁后，锁才会被移除
     * - 使用remove(key, wrapper)确保只移除指定的wrapper，避免并发问题
     * 
     * 线程安全：
     * - usageCount使用AtomicInteger，确保原子操作
     * - lockMap是ConcurrentHashMap，支持并发操作
     * - remove操作使用CAS机制，确保线程安全
     * 
     * 注意事项：
     * - 引用计数减到0时才会移除锁，避免正在使用的锁被移除
     * - 使用remove(key, wrapper)而不是remove(key)，确保只移除指定的wrapper实例
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
