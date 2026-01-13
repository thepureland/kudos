package io.kudos.context.lock

import io.kudos.base.lang.ThreadKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.KeyLockRegistry
import java.util.concurrent.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * 普通锁服务实现
 * 
 * 基于内存的锁服务，支持锁的自动过期和清理。
 * 
 * 核心特性：
 * 1. 锁过期机制：使用DelayQueue实现锁的自动过期
 * 2. 守护线程清理：后台守护线程自动清理过期的锁
 * 3. 可重入锁：使用KeyLockRegistry提供可重入锁功能
 * 4. 过期时间：支持设置锁的过期时间（秒）
 * 
 * 锁存储：
 * - cacheKeyMap：存储锁key和过期时间戳的映射
 * - delayQueue：延迟队列，用于自动清理过期锁
 * 
 * 过期清理：
 * - 守护线程持续监听delayQueue
 * - 当锁过期时，自动从cacheKeyMap中移除
 * - 确保过期的锁不会一直占用内存
 * 
 * 注意事项：
 * - 使用守护线程，不会阻止JVM关闭
 * - 锁过期后会自动释放，无需手动解锁
 * - 支持并发访问，线程安全
 */
class NormalLockService : ILockProvider<ReentrantLock> {
    private val cacheKeyMap: ConcurrentMap<String?, Long?> = ConcurrentHashMap()
    private val delayQueue: DelayQueue<ExpiringKey<String?>> = DelayQueue()

    private val reentrantLockManager = KeyLockRegistry<String>()
    private val log = LogFactory.getLog(NormalLockService::class.java)

    init {
        /**
         * 初始化守护线程，用于自动清理过期的锁
         * 
         * 工作流程：
         * 1. 创建守护线程，设置为daemon线程
         * 2. 持续监听delayQueue，等待过期的锁
         * 3. 当有锁过期时，从cacheKeyMap中移除
         * 4. 如果线程被中断，恢复中断状态并退出
         * 
         * 守护线程特性：
         * - daemon线程不会阻止JVM关闭
         * - 当所有非daemon线程结束时，JVM会关闭
         * - 适合后台清理任务
         * 
         * 阻塞机制：
         * - delayQueue.take()会阻塞直到有元素过期
         * - 不会消耗CPU资源，高效等待
         * 
         * 异常处理：
         * - 捕获InterruptedException，恢复中断状态
         * - 确保线程能够正确响应中断信号
         */
        //守护线程删除过期key
        Thread(Runnable {
            try {
                while (true) {
                    // 阻塞直到有过期 key
                    val expKey = delayQueue.take()
                    cacheKeyMap.remove(expKey.key) // 只有值匹配时才删除
                }
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }).start()
    }

    /**
     * 缓存锁（设置过期时间）
     * 
     * 尝试获取锁并设置过期时间，如果获取成功则锁会在指定时间后自动过期。
     * 
     * @param key 锁的key
     * @param seconds 过期时间（秒）
     */
    fun doCache(key: String, seconds: Int) {
        tryLock(key, seconds)
    }

    /**
     * 检查key是否存在
     * 
     * 检查指定的key是否在锁映射中存在（即锁是否被持有）。
     * 
     * @param key 锁的key
     * @return true表示锁存在，false表示锁不存在
     */
    fun hasKey(key: String?): Boolean {
        return cacheKeyMap.containsKey(key)
    }

    /**
     * 获取锁对象
     * 
     * 尝试非阻塞地获取指定key的锁对象。
     * 
     * @param key 锁的key
     * @return ReentrantLock对象，如果获取失败返回null
     */
    override fun lock(key: String): ReentrantLock? {
        return reentrantLockManager.tryLock(key)
    }

    /**
     * 释放锁
     * 
     * 释放通过lock方法获取的锁对象。
     * 
     * @param lock 锁对象
     * @param key 锁的key
     */
    override fun unLock(lock: Lock, key: String) {
        unLock(key)
    }

    /**
     * 释放锁
     * 
     * 释放指定key的锁。
     * 
     * @param key 锁的key
     */
    override fun unLock(key: String) {
        this.reentrantLockManager.unlock(key)
    }

    /**
     * 尝试获取锁（带过期时间）
     * 
     * 尝试获取指定key的锁，如果获取成功则设置过期时间。
     * 
     * 工作流程：
     * 1. 检查锁是否存在：如果key已存在，说明锁已被持有，返回false
     * 2. 计算过期时间：当前时间 + 过期秒数
     * 3. 原子性放入：使用putIfAbsent原子性地放入key和过期时间
     * 4. 判断结果：
     *    - 如果old为null，说明是第一个线程，放入成功，加入延迟队列，返回true
     *    - 如果old不为null，说明其他线程已先放入，返回false
     * 
     * 并发安全：
     * - 使用ConcurrentHashMap保证线程安全
     * - putIfAbsent是原子操作，确保只有一个线程能成功放入
     * - 第一个成功的线程负责将key加入延迟队列
     * 
     * 过期机制：
     * - 锁的过期时间存储在cacheKeyMap中
     * - 过期时间到达后，守护线程会自动清理
     * - 锁过期后，其他线程可以重新获取
     * 
     * 返回值：
     * - true：成功获取锁
     * - false：锁已被其他线程持有
     * 
     * 注意事项：
     * - 锁会在指定时间后自动过期，无需手动释放
     * - 如果锁已过期，可以重新获取
     * - 使用putIfAbsent确保并发安全
     * 
     * @param lockKey 锁的key
     * @param sec 锁的过期时间（秒）
     * @return true表示成功获取锁，false表示锁已被持有
     */
    override fun tryLock(lockKey: String, sec: Int): Boolean {
        if (cacheKeyMap.containsKey(lockKey)) {
            return false
        }
        val expireTime = System.currentTimeMillis() + (sec * 1000)
        //如果key不存在，则返回旧的值空，如果key存在，则不处理
        val old = cacheKeyMap.putIfAbsent(lockKey, expireTime)
        if (old == null) {
            // 第一个线程进来，key 还不存在，真正放入，并加入延迟队列
            delayQueue.put(ExpiringKey<String?>(lockKey, expireTime))
            return true
        }
        return false
    }

    /**
     * 过期键包装类
     * 
     * 实现Delayed接口，用于DelayQueue中存储带过期时间的键。
     * 
     * 核心功能：
     * 1. 存储键和过期时间：保存键对象和过期时间戳
     * 2. 计算剩余时间：实现getDelay方法，计算距离过期的剩余时间
     * 3. 比较排序：实现compareTo方法，用于DelayQueue排序
     * 4. 相等判断：仅按key判断相等，用于remove操作
     * 
     * 过期时间：
     * - 使用绝对时间戳（毫秒）
     * - 计算公式：System.currentTimeMillis() + delayMillis
     * - 存储在expireAtMillis字段中
     * 
     * DelayQueue使用：
     * - DelayQueue会根据getDelay的返回值排序
     * - 过期时间越早的元素越靠前
     * - take()方法会阻塞直到有元素过期
     * 
     * 相等性：
     * - equals和hashCode仅基于key
     * - 允许同一个key有多个ExpiringKey实例（不同过期时间）
     * - remove操作时可以通过key匹配删除
     * 
     * @param K 键的类型
     * @param key 键对象
     * @param expireAtMillis 到期的绝对时刻（毫秒）
     */
    private class ExpiringKey<K>(
        val key: K?,
        /** 到期的绝对时刻，单位毫秒（System.currentTimeMillis() + delayMillis）  */
        private val expireAtMillis: kotlin.Long
    ) : Delayed {

        /**
         * 计算距离过期的剩余时间
         * 
         * 计算当前时间到过期时间的差值，并转换为指定的时间单位。
         * 
         * 计算方式：
         * - 剩余毫秒 = 过期时间戳 - 当前时间戳
         * - 将毫秒转换为指定的时间单位
         * - 如果已过期，返回负数或0
         * 
         * 时间单位转换：
         * - 使用TimeUnit.convert进行单位转换
         * - 支持所有TimeUnit定义的时间单位
         * 
         * @param unit 时间单位
         * @return 距离过期的剩余时间（指定单位），如果已过期则<=0
         */
        override fun getDelay(unit: TimeUnit): kotlin.Long {
            val remainingMillis = expireAtMillis - System.currentTimeMillis()
            // 将毫秒差值转换成调用者需要的时间单位
            return unit.convert(remainingMillis, TimeUnit.MILLISECONDS)
        }

        /**
         * 比较两个Delayed对象的过期时间
         * 
         * 用于DelayQueue的排序，过期时间越早的元素越靠前。
         * 
         * 比较方式：
         * - 统一转换为毫秒进行比较
         * - 计算两个对象的剩余时间差值
         * - 返回差值的符号（-1、0、1）
         * 
         * 排序规则：
         * - 过期时间早的元素排在前面
         * - 过期时间相同的元素顺序不确定
         * - DelayQueue会优先取出过期时间早的元素
         * 
         * @param other 另一个Delayed对象
         * @return 负数表示当前对象过期更早，0表示同时过期，正数表示当前对象过期更晚
         */
        override fun compareTo(other: Delayed): Int {
            // 统一按毫秒比较到期时间
            val diff = this.getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS)
            return diff.compareTo(0L)
        }

        /**
         * 判断两个ExpiringKey是否相等
         * 
         * 仅按key判断相等，不考虑过期时间。
         * 
         * 相等规则：
         * - 如果是同一个对象，返回true
         * - 如果类型不同，返回false
         * - 如果key相等，返回true
         * 
         * 设计原因：
         * - 允许同一个key有多个ExpiringKey实例
         * - remove操作时可以通过key匹配删除
         * - 简化过期键的管理
         * 
         * @param other 另一个对象
         * @return true表示相等，false表示不相等
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExpiringKey<*>) return false
            val that = other
            // 仅按 key 比较，以便 remove 时匹配
            return key == that.key
        }

        /**
         * 计算哈希码
         * 
         * 仅基于key计算哈希码，与equals方法保持一致。
         * 
         * @return key的哈希码
         */
        override fun hashCode(): Int {
            return key.hashCode()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val lockService = NormalLockService()
            lockService.tryLock("lock1", 3)
            ThreadKit.sleep(15000)
        }
    }
}
