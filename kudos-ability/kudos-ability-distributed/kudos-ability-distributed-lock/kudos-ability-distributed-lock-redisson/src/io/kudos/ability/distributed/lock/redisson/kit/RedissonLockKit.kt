package io.kudos.ability.distributed.lock.redisson.kit

import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import io.kudos.context.kit.SpringKit
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

/**
 * 分布式锁工具类，目前使用redisson实现
 */
object RedissonLockKit {

    private var lockBean: RedissonLocker? = null

    private const val LOCK_KEY_PREFIX = "REDISSON::"

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return RLock
     */
    fun getLock(lockKey: String): RLock {
        initLockBean()
        return lockBean!!.getLock(getLockKey(lockKey))
    }

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return RLock
     */
    fun lock(lockKey: String): RLock {
        initLockBean()
        return lockBean!!.lock(getLockKey(lockKey))
    }

    /**
     * 获取分布式锁，并指定锁失效秒数
     *
     * @param lockKey lockKey
     * @param timeOut timeOut
     * @return RLock
     */
    fun lock(lockKey: String, timeOut: Long): RLock {
        initLockBean()
        return lockBean!!.lock(getLockKey(lockKey), timeOut)
    }

    /**
     * 获取分布式锁，并指定锁失效时间
     *
     * @param lockKey lockKey
     * @param unit    unit
     * @param timeOut timeOut
     * @return RLock
     */
    fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): RLock {
        initLockBean()
        return lockBean!!.lock(getLockKey(lockKey), unit, timeOut)
    }

    /**
     * 尝试获取锁，如果获取成功返回true，否则返回false
     *
     * @param lockKey   lockKey
     * @param unit      unit
     * @param timeOut   获取锁等待时间
     * @param leaseTime 获取锁成功后，锁失效时间
     */
    fun tryLock(lockKey: String, unit: TimeUnit, timeOut: Long, leaseTime: Long): Boolean {
        initLockBean()
        return lockBean!!.tryLock(getLockKey(lockKey), unit, timeOut, leaseTime)
    }

    /**
     * 解除分布式锁
     *
     * @param lockKey lockKey
     */
    fun unlock(lockKey: String) {
        initLockBean()
        lockBean!!.unlock(getLockKey(lockKey))
    }

    /**
     * 解除分布式锁
     *
     * @param lock lock
     */
    fun unlock(lock: RLock) {
        initLockBean()
        if (lock.isLocked()) {
            lock.unlock()
        }
    }

    @Synchronized
    private fun initLockBean() {
        if (lockBean == null) {
            lockBean = SpringKit.getBean(RedissonLocker::class)
        }
    }

    const val REDISSON_CLIENT_BEAN_NAME: String = "redissonClient"

    fun redissonClient(): RedissonClient {
        return SpringKit.getBean(REDISSON_CLIENT_BEAN_NAME) as RedissonClient
    }

    fun getLockKey(key: String): String {
        return LOCK_KEY_PREFIX + key
    }
}
