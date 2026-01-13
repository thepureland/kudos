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
     * 尝试获取锁，如果获取成功返回true，否则返回false
     *
     * @param lockKey   lockKey
     * @param unit      unit
     * @param timeOut  获取锁等待时间
     * @param leaseTime 获取锁成功后，锁失效时间
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
