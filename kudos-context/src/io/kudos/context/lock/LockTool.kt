package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.context.kit.SpringKit
import java.util.concurrent.locks.Lock
import java.util.function.Supplier

object LockTool {
    private lateinit var LOCK_SERVICE: ILockProvider<out Lock>

    init {
        initLockService()
    }

    /**
     * 在 [second] 秒窗口内尝试获取租约式锁；若 **未能** 取得（即 [ILockProvider.tryLock] 为 false），返回 `true`。
     * 语义等价于「当前 key 已被占用 / 本线程未拿到锁」，与 `tryLock` 布尔值取反。
     */
    fun hasKeyLock(lockKey: String, second: Int): Boolean = !LOCK_SERVICE.tryLock(lockKey, second)

    fun <T> lockExecute(lockKey: String, supplier: Supplier<T?>, errorCode: IErrorCodeEnum): T? {
        //如果90秒内没释放，则自动释放
        return lockExecute<T?>(lockKey, supplier, 90, errorCode)
    }

    fun <T> lockExecute(lockKey: String, supplier: Supplier<T?>, second: Int, errorCode: IErrorCodeEnum): T? {
        return lockProvider.lockExecute(lockKey, supplier, second, errorCode)
    }

    fun lockExecute(lockKey: String, runnable: Runnable, errorCode: IErrorCodeEnum) {
        //如果90秒内没释放，则自动释放
        lockExecute(lockKey, runnable, 90, errorCode)
    }

    fun lockExecute(lockKey: String, runnable: Runnable, second: Int, errorCode: IErrorCodeEnum) {
        lockProvider.lockExecute(lockKey, runnable, second, errorCode)
    }

    val lockProvider: ILockProvider<out Lock>
        get() = LOCK_SERVICE

    private fun initLockService() {
        val beansOfType = SpringKit.getBeansOfType<ILockProvider<*>>()
        LOCK_SERVICE = if (beansOfType.isEmpty()) {
            NormalLockService()
        } else {
            beansOfType.values.minBy { it.order() }
        }
    }
}
