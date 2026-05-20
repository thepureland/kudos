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
    fun getLock(lockKey: String): RLock = locker().getLock(getLockKey(lockKey))

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return RLock
     */
    fun lock(lockKey: String): RLock = locker().lock(getLockKey(lockKey))

    /**
     * 获取分布式锁，并指定锁失效秒数
     *
     * @param lockKey lockKey
     * @param timeOut timeOut
     * @return RLock
     */
    fun lock(lockKey: String, timeOut: Long): RLock = locker().lock(getLockKey(lockKey), timeOut)

    /**
     * 获取分布式锁，并指定锁失效时间
     *
     * @param lockKey lockKey
     * @param unit    unit
     * @param timeOut timeOut
     * @return RLock
     */
    fun lock(lockKey: String, unit: TimeUnit, timeOut: Long): RLock =
        locker().lock(getLockKey(lockKey), unit, timeOut)

    /**
     * 尝试获取锁，如果获取成功返回true，否则返回false
     *
     * @param lockKey   lockKey
     * @param unit      unit
     * @param timeOut   获取锁等待时间
     * @param leaseTime 获取锁成功后，锁失效时间
     */
    fun tryLock(lockKey: String, unit: TimeUnit, timeOut: Long, leaseTime: Long): Boolean =
        locker().tryLock(getLockKey(lockKey), unit, timeOut, leaseTime)

    /**
     * 解除分布式锁
     *
     * @param lockKey lockKey
     */
    fun unlock(lockKey: String) {
        locker().unlock(getLockKey(lockKey))
    }

    /**
     * 解除分布式锁。
     *
     * 与 [unlock] 字符串版的差异：直接释放传入的 [RLock] 对象，少一次按 key 查锁的 RTT；
     * **但必须确认本线程持有该锁**，否则 Redisson 会抛 `IllegalMonitorStateException`。
     * 旧实现只检查 `isLocked`，对"别的线程持有这把锁"的场景会报错。已加上
     * `isHeldByCurrentThread` 双重检查与 [RedissonLocker.unlock] 行为对齐。
     */
    fun unlock(lock: RLock) {
        if (lock.isLocked && lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }

    /**
     * 惰性初始化 [lockBean]。
     * 加 `@Synchronized` 是因为 object 静态字段在多线程环境下首次访问可能并发命中——
     * Spring 容器初始化前后切换的窗口期，单纯的 `?.let` 不够安全。
     *
     * @author K
     * @since 1.0.0
     */
    @Synchronized
    private fun initLockBean() {
        if (lockBean == null) {
            lockBean = SpringKit.getBean<RedissonLocker>()
        }
    }

    /**
     * 取已初始化的 [RedissonLocker]；未就绪时立即报错（说明 Spring 容器还没装好就被调用）。
     *
     * @return [RedissonLocker] 单例
     * @throws IllegalArgumentException Spring 容器尚未注入 [RedissonLocker] 时
     * @author K
     * @since 1.0.0
     */
    private fun locker(): RedissonLocker {
        initLockBean()
        return requireNotNull(lockBean) { "RedissonLocker 未就绪" }
    }

    /** Spring 容器中 Redisson 客户端 bean 名 */
    const val REDISSON_CLIENT_BEAN_NAME: String = "redissonClient"

    /**
     * 从 Spring 容器拿 [RedissonClient] bean。
     * 业务侧需要原生 Redisson API（如 `RBucket` / `RBlockingQueue`）时使用。
     *
     * @return Redisson 客户端
     * @author K
     * @since 1.0.0
     */
    fun redissonClient(): RedissonClient =
        SpringKit.getBean(REDISSON_CLIENT_BEAN_NAME) as RedissonClient

    /**
     * 把业务 key 拼上 [LOCK_KEY_PREFIX] 形成 Redis 中的完整锁 key。
     * 集中前缀让运维通过 key 模式就能识别 redisson 锁。
     *
     * @param key 业务 key
     * @return 带 `REDISSON::` 前缀的最终 key
     * @author K
     * @since 1.0.0
     */
    fun getLockKey(key: String): String = LOCK_KEY_PREFIX + key
}
