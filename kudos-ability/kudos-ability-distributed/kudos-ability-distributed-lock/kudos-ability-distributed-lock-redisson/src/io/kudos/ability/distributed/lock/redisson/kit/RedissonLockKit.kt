package io.kudos.ability.distributed.lock.redisson.kit

import org.redisson.api.RLock
import org.soul.ability.distributed.lock.redisson.RedissonLockTool
import java.util.concurrent.TimeUnit


/**
 * redisson分布式锁工具类
 *
 * @author K
 * @since 1.0.0
 */
object RedissonLockKit {

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return RLock
     */
    fun getLock(lockKey: String): RLock = RedissonLockTool.getLock(lockKey)

    /**
     * 加锁
     *
     * @param lockKey 锁的key
     * @return RLock
     */
    fun lock(lockKey: String): RLock = RedissonLockTool.lock(lockKey)

    /**
     * 加计时锁，超时释放锁
     *
     * @param lockKey 锁的key
     * @param timeout 超时秒数
     * @return RLock
     */
    fun lock(lockKey: String, timeout: Int): RLock = RedissonLockTool.lock(lockKey, timeout)

    /**
     * 加计时锁，超时释放锁
     *
     * @param lockKey 锁的key
     * @param unit    计时单位
     * @param timeout 超时时间
     * @return RLock
     */
    fun lock(lockKey: String, unit: TimeUnit, timeout: Int): RLock = RedissonLockTool.lock(lockKey, unit, timeout)

    /**
     * 尝试加锁
     *
     * @param lockKey   锁的key
     * @param unit      计时单位
     * @param waitTime  获取锁等待时间
     * @param timeout 获取锁成功后，锁失效时间
     * @return 获取成功返回true，否则返回false
     */
    fun tryLock(lockKey: String, unit: TimeUnit, waitTime: Int, timeout: Int): Boolean = RedissonLockTool.tryLock(lockKey, unit, waitTime,timeout)

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    fun unlock(lockKey: String) = RedissonLockTool.unlock(lockKey)

    /**
     * 释放锁
     *
     * @param lock 锁对象
     */
    fun unlock(lock: RLock) = RedissonLockTool.unlock(lock)

}