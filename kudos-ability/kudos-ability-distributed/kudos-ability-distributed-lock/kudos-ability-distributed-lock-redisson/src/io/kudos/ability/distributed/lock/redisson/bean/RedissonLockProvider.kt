package io.kudos.ability.distributed.lock.redisson.bean

import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.context.lock.ILockProvider
import org.redisson.api.RLock
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

/**
 * Redisson lock provider.
 * Implements ILockProvider to expose Redisson-backed distributed lock functionality.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedissonLockProvider : ILockProvider<RLock> {
    
    override fun lock(key: String): RLock? = RedissonLockKit.lock(key)

    override fun unLock(lock: Lock, key: String) {
        if (lock is RLock) {
            if (lock.name == RedissonLockKit.getLockKey(key)) {
                RedissonLockKit.unlock(lock)
            }
        } else {
            lock.unlock()
        }
    }

    override fun unLock(key: String) {
        RedissonLockKit.unlock(key)
    }

    override fun order(): Int = 90

    override fun tryLock(lockKey: String, sec: Int): Boolean =
        RedissonLockKit.tryLock(lockKey, TimeUnit.SECONDS, 0, sec.toLong())
}
