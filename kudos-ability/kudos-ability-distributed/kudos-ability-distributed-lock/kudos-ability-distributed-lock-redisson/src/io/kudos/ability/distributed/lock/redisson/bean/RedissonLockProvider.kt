package io.kudos.ability.distributed.lock.redisson.bean

import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.context.lock.ILockProvider
import org.redisson.api.RLock
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

class RedissonLockProvider : ILockProvider<RLock> {
    
    override fun lock(key: String): RLock? {
        return RedissonLockKit.lock(key)
    }

    override fun unLock(lock: Lock, key: String) {
        lock.unlock()
    }

    override fun unLock(key: String) {
        RedissonLockKit.unlock(key)
    }

    override fun order(): Int {
        return 90
    }

    override fun tryLock(lockKey: String, second: Int): Boolean {
        return RedissonLockKit.tryLock(lockKey, TimeUnit.SECONDS, 0, second.toLong())
    }
}
