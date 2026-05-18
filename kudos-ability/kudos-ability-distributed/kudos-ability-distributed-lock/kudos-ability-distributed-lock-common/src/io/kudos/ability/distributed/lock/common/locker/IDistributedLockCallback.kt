package io.kudos.ability.distributed.lock.common.locker

/**
 * 分布式锁回调 SPI。
 *
 * `@DistributedLock` 注解的切面（在 lock-redisson 模块实现）拿锁前后调用本接口的方法；
 * 业务侧通过 `DistributedLockContext.set(callback)` 把自己的回调实例塞入 ThreadLocal，
 * 切面读出来调用。
 *
 * @author K
 * @since 1.0.0
 */
interface IDistributedLockCallback {
    /**
     * 成功上锁增加处理
     * @param lockKey
     */
    fun doLockSuccess(lockKey: String) {}

    /**
     * 上锁失败处理
     * @param lockKey
     */
    fun doLockFail(lockKey: String)
}
