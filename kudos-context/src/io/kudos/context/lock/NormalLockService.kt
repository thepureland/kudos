package io.kudos.context.lock

import io.kudos.base.lang.ThreadKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.KeyLockRegistry
import java.lang.Long
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.hashCode

class NormalLockService : ILockProvider<ReentrantLock> {
    private val cacheKeyMap: ConcurrentMap<String?, Long?> = ConcurrentHashMap<String?, Long?>()
    private val delayQueue: DelayQueue<ExpiringKey<String?>> = DelayQueue<ExpiringKey<String?>>()

    private val reentrantLockManager = KeyLockRegistry<String>()
    private val log = LogFactory.getLog(NormalLockService::class.java)

    init {
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

    fun doCache(key: String, seconds: Int) {
        tryLock(key, seconds)
    }

    fun hasKey(key: String?): Boolean {
        return cacheKeyMap.containsKey(key)
    }

    override fun lock(key: String): ReentrantLock? {
        return reentrantLockManager.tryLock(key)
    }

    override fun unLock(lock: Lock, key: String) {
        unLock(key)
    }

    override fun unLock(key: String) {
        this.reentrantLockManager.unlock(key)
    }

    override fun tryLock(lockKey: String, second: Int): Boolean {
        if (cacheKeyMap.containsKey(lockKey)) {
            return false
        }
        val expireTime = System.currentTimeMillis() + (second * 1000)
        //如果key不存在，则返回旧的值空，如果key存在，则不处理
        val old = cacheKeyMap.putIfAbsent(lockKey, expireTime as Long)
        if (old == null) {
            // 第一个线程进来，key 还不存在，真正放入，并加入延迟队列
            delayQueue.put(ExpiringKey<String?>(lockKey, expireTime))
            return true
        }
        return false
    }

    private class ExpiringKey<K>(
        val key: K?,
        /** 到期的绝对时刻，单位毫秒（System.currentTimeMillis() + delayMillis）  */
        private val expireAtMillis: kotlin.Long
    ) : Delayed {

        override fun getDelay(unit: TimeUnit): kotlin.Long {
            val remainingMillis = expireAtMillis - System.currentTimeMillis()
            // 将毫秒差值转换成调用者需要的时间单位
            return unit.convert(remainingMillis, TimeUnit.MILLISECONDS)
        }

        override fun compareTo(other: Delayed): Int {
            // 统一按毫秒比较到期时间
            val diff = this.getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS)
            return diff.compareTo(0L)
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj !is ExpiringKey<*>) return false
            val that = obj
            // 仅按 key 比较，以便 remove 时匹配
            return key == that.key
        }

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
