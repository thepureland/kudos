package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.error.ServiceException
import java.util.concurrent.locks.Lock

/**
 * 租约式（lease-based）锁接口。
 *
 * 行为：[tryLock] 在 `sec` 秒内尝试获取锁，**取到后自动过期**，调用方一般用 [lockExecute]
 * 拿到自动 try-finally 释放；也可显式 [unLock] 提前释放。
 *
 * 与 [IReentrantLockProvider] 的差别：
 * - 租约锁通过过期时间保护，**忘记释放不会导致永久死锁**
 * - 不支持可重入（同一线程二次 [tryLock] 同一 key 会失败）
 * - [lockExecute] 仅在此接口上提供
 *
 * @author K
 * @since 1.0.0
 */
interface ILeaseLockProvider {

    /**
     * 在 `sec` 秒窗口内尝试获取 [lockKey] 的租约锁。租约到期后自动失效。
     * @return true 表示获取成功，false 表示该 key 已被占用
     */
    fun tryLock(lockKey: String, sec: Int): Boolean

    /** 释放租约锁。当 lockExecute 自动释放就不需要再调用 */
    fun unLock(key: String)

    /**
     * 上锁执行，执行完毕后自动释放锁。
     *
     * - 取到锁 → 执行 [supplier]，finally 中 [unLock]
     * - 取锁失败 + [errorCode] != null → 抛 [ServiceException]
     * - 取锁失败 + [errorCode] == null → 返回 null
     */
    fun <T> lockExecute(
        lockKey: String,
        supplier: java.util.function.Supplier<T?>,
        sec: Int,
        errorCode: IErrorCodeEnum?
    ): T? {
        if (!tryLock(lockKey, sec)) {
            if (errorCode != null) throw ServiceException(errorCode)
            return null
        }
        return try {
            supplier.get()
        } finally {
            unLock(lockKey)
        }
    }

    /** Runnable 重载，无返回值。语义与上面相同。 */
    fun lockExecute(lockKey: String, runnable: Runnable, sec: Int, errorCode: IErrorCodeEnum?) {
        if (!tryLock(lockKey, sec)) {
            if (errorCode != null) throw ServiceException(errorCode)
            return
        }
        try {
            runnable.run()
        } finally {
            unLock(lockKey)
        }
    }
}

/**
 * 可重入（reentrant）锁接口。
 *
 * 行为：[lock] 返回一个 [Lock] 实例，调用方需 **手动** [unLock]。同一线程对同一 key 可重入。
 *
 * 与 [ILeaseLockProvider] 的差别：
 * - **不带超时**——忘记释放就死锁
 * - 支持可重入
 * - 适合"双重检查锁"等需要在锁内做条件判断的场景
 *
 * @author K
 * @since 1.0.0
 */
interface IReentrantLockProvider<L : Lock> {

    /** 获取 [key] 对应的可重入锁。null 表示获取失败（实现自行决定语义） */
    fun lock(key: String): L?

    /** 释放可重入锁。[lock] 参数仅用于配合需要持锁对象的实现（如 RedissonLock）；本地实现可忽略 */
    fun unLock(lock: Lock, key: String)
}

/**
 * 同时提供租约锁与可重入锁的复合锁提供者。
 *
 * **历史背景**：原 ILockProvider 把两套语义糅在一个接口里，KDoc 注明"两套机制；
 * lockExecute 仅用 tryLock，不走 lock 返回的锁对象"——很容易被误用。重构后两套
 * 语义拆为 [ILeaseLockProvider] 与 [IReentrantLockProvider]，这里把两者并起来
 * 作为兼容 marker：[NormalLockService] / [RedissonLockProvider] 等现有实现继续
 * 实现 [ILockProvider]，老调用方（如 LockTool / DistributedCacheGuardAspect）
 * 无需改动；**新代码**应优先依赖具体的子接口表达意图。
 *
 * @author K
 * @since 1.0.0
 */
interface ILockProvider<L : Lock> : ILeaseLockProvider, IReentrantLockProvider<L> {

    /** 优先级顺序，值越小顺序越大 */
    fun order(): Int = 99

    companion object {
        const val BEAN_NAME: String = "failedLockProvider"
    }
}
