package io.kudos.ability.distributed.lock.redisson.locker

import io.kudos.ability.distributed.lock.common.locker.ILocker
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

/**
 * Redisson分布式锁实现
 * 基于Redisson实现分布式锁功能，提供获取锁、加锁、解锁等操作
 */
class RedissonLocker : ILocker<RLock> {

    @Autowired(required = false)
    private lateinit var redissonClient: RedissonClient

    /**
     * 获取分布式锁对象
     *
     * @param lockKey
     */
    override fun getLock(lockKey: String): RLock {
        return this.redissonClient.getLock(lockKey)
    }

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return RLock
     */
    override fun lock(lockKey: String): RLock {
        val lock: RLock = this.redissonClient.getLock(lockKey)
        lock.lock()
        return lock
    }

    /**
     * 获取分布式锁，并指定锁失效秒数
     *
     * @param lockKey lockKey
     * @param timeOut timeOut
     * @return RLock
     */
    override fun lock(lockKey: String, timeOut: Long): RLock {
        val lock: RLock = this.redissonClient.getLock(lockKey)
        lock.lock(timeOut, TimeUnit.SECONDS)
        return lock
    }

    /**
     * 获取分布式锁，并指定锁失效时间
     *
     * @param lockKey lockKey
     * @param unit    unit
     * @param timeOut timeOut
     * @return RLock
     */
    override fun lock(
        lockKey: String,
        unit: TimeUnit,
        timeOut: Long
    ): RLock {
        val lock: RLock = this.redissonClient.getLock(lockKey)
        lock.lock(timeOut, unit)
        return lock
    }

    /**
     * 尝试获取分布式锁
     * 
     * 在指定时间内尝试获取锁，如果获取成功则设置锁的租约时间。
     * 
     * 工作流程：
     * 1. 获取RLock对象
     * 2. 调用tryLock方法尝试获取锁：
     *    - 在timeOut时间内等待获取锁
     *    - 如果获取成功，设置锁的租约时间为leaseTime
     *    - 如果获取失败或超时，返回false
     * 3. 处理中断异常：如果线程被中断，返回false
     * 
     * 参数说明：
     * - timeOut：获取锁的等待时间，在此时长内会持续尝试获取锁
     * - leaseTime：获取锁成功后的租约时间，超过此时长锁会自动释放
     * - unit：时间单位，同时应用于timeOut和leaseTime
     * 
     * 返回值：
     * - true：成功获取锁
     * - false：获取锁失败（超时、被中断或其他原因）
     * 
     * 注意事项：
     * - 如果线程在等待过程中被中断，会捕获InterruptedException并返回false
     * - 获取锁成功后，需要在leaseTime内完成业务逻辑并释放锁
     * - 如果业务逻辑执行时间超过leaseTime，锁会自动释放，可能导致并发问题
     * 
     * @param lockKey 锁的key
     * @param unit 时间单位
     * @param timeOut 获取锁等待时间
     * @param leaseTime 获取锁成功后的租约时间（锁失效时间）
     * @return true表示成功获取锁，false表示获取锁失败
     */
    override fun tryLock(
        lockKey: String,
        unit: TimeUnit,
        timeOut: Long,
        leaseTime: Long
    ): Boolean {
        val lock: RLock = this.redissonClient.getLock(lockKey)
        var result: Boolean
        try {
            result = lock.tryLock(timeOut, leaseTime, unit)
        } catch (var8: java.lang.InterruptedException) {
            result = false
        }
        return result
    }

    /**
     * 解除分布式锁
     *
     * @param lockKey lockKey
     */
    override fun unlock(lockKey: String) {
        val lock: RLock = this.redissonClient.getLock(lockKey)
        lock.unlock()
    }

    /**
     * 解除分布式锁
     *
     * @param lock lockKey
     */
    override fun unlock(lock: RLock) {
        lock.unlock()
    }
}
