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
 * 普通锁服务实现
 *
 * **实现说明**：[tryLock]（租约键 + [DelayQueue] 过期）与 [lock]/[unLock]（[KeyLockRegistry] 内 [ReentrantLock]）
 * 为两套机制；[ILockProvider.lockExecute] 及 [LockTool] 常用路径只使用 [tryLock]，与「按 key 取 [ReentrantLock]」无联动。
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

    private val cacheKeyMap = ConcurrentHashMap<String, Long>()

    private val delayQueue = DelayQueue<ExpiringKey<String?>>()

    private val reentrantLockManager = KeyLockRegistry<String>()

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
        // 守护线程删除过期 key（与文档一致：daemon 不阻止 JVM 退出）
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
    fun hasKey(key: String?): Boolean = cacheKeyMap.containsKey(key)

    /**
     * 获取锁对象
     * 
     * 尝试非阻塞地获取指定key的锁对象。
     * 
     * @param key 锁的key
     * @return ReentrantLock对象，如果获取失败返回null
     */
    override fun lock(key: String): ReentrantLock? = reentrantLockManager.tryLock(key)

    /**
     * 释放**可重入锁**（与 [lock] 配套）。
     *
     * 历史 bug：旧实现 `unLock(lock, key) = unLock(key)` 把它委托给租约锁释放路径，
     * 但 [lock] 写入的是 [reentrantLockManager]、不是 [cacheKeyMap]——
     * 走 [reentrantLockManager.unlock] 才是配对的释放方式。
     */
    override fun unLock(lock: Lock, key: String) {
        this.reentrantLockManager.unlock(key)
    }

    /**
     * 释放**租约锁**（与 [tryLock] 配套，对应 [lockExecute] 的 finally 释放路径）。
     *
     * 历史 bug：旧实现 `unLock(key) = reentrantLockManager.unlock(key)`，但 [tryLock]
     * 写入的是 [cacheKeyMap]——`reentrantLockManager` 里压根没条目，必抛
     * "No lock found"。这导致 [lockExecute] 自创建以来一直 broken。
     *
     * 现修复为：
     * 1. 从 [cacheKeyMap] 移除 key
     * 2. 显式从 [delayQueue] 撤掉对应条目，避免守护线程之后误清"同名 key 重新占用"
     *    的新锁（依赖 [ExpiringKey.equals] 只比 key 不比 expireAtMillis 的设计）
     */
    override fun unLock(key: String) {
        val previousExpire = cacheKeyMap.remove(key)
        if (previousExpire != null) {
            // 守护线程之后处理这个旧条目时会再 remove 一次 cacheKeyMap[key]，
            // 那时如果 key 已被同名新锁重新占用，会把新锁误清——所以主动撤掉
            delayQueue.remove(ExpiringKey(key, previousExpire))
        }
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
        val expireTime = System.currentTimeMillis() + (sec * 1000)
        // 旧实现先 containsKey 再 putIfAbsent——前面那次检查冗余：
        // putIfAbsent 本身原子地完成"检查+插入"，并通过返回值告知是否成功。
        // 删掉冗余检查让逻辑更紧凑、并发下少一次 map 访问。
        val old = cacheKeyMap.putIfAbsent(lockKey, expireTime)
        return if (old == null) {
            // 本线程是首个成功插入者，加入延迟队列让守护线程到期清理
            delayQueue.put(ExpiringKey(lockKey, expireTime))
            true
        } else {
            false
        }
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
        private val expireAtMillis: Long
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
        override fun getDelay(unit: TimeUnit): Long {
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
        override fun compareTo(other: Delayed): Int =
            getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))

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
            // 仅按 key 比较，以便 remove 时匹配
            return key == other.key
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

}
