package io.kudos.ability.distributed.lock.common.locker

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

/**
 * 分布式锁 SPI。具体实现见 `kudos-ability-distributed-lock-redisson`。
 *
 * 设计意图：让业务代码 `@Autowired ILocker<*>` 后调用 `lock(key, ...)` / `unlock(key)`，
 * 不必接触 Redisson / Curator / Etcd 等具体客户端。
 *
 * @param T 锁类型，继承自标准 [Lock]——具体实现通常返回 `RLock`（Redisson）或类似可重入锁对象
 * @author K
 * @since 1.0.0
 */
interface ILocker<T : Lock?> {
    /** 仅返回锁对象引用（不获取锁）。返回 null 表示无法构造锁。 */
    fun getLock(lockKey: String): T?

    /**
     * 获取锁的便捷签名。
     *
     * 实现不应无限阻塞；无法在默认等待窗口内拿到锁时应返回 null。需要明确等待时间 / 租期的业务
     * 应优先调用 [tryLock]。
     */
    fun lock(lockKey: String): T?

    /** 阻塞获取锁，秒级超时。 */
    fun lock(lockKey: String, timeOut: Long): T?

    /** 阻塞获取锁，自定义时间单位 + 超时。 */
    fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): T?

    /**
     * 非阻塞尝试获取锁，含租期（leaseTime）——超过租期自动释放，避免持锁线程挂掉导致永远不释放。
     *
     * @return true 表示获取成功；false 表示在 timeOut 内未取到
     */
    fun tryLock(lockKey: String, unit: TimeUnit, timeOut: Long, leaseTime: Long): Boolean

    /** 按 key 释放锁——调用方负责保证释放的是自己持有的锁。 */
    fun unlock(lockKey: String)

    /** 直接释放锁对象——避免 [unlock] 按 key 再查一次。 */
    fun unlock(lock: T)
}
