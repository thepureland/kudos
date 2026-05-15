package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.context.kit.SpringKit
import java.util.concurrent.locks.Lock
import java.util.function.Supplier

object LockTool {

    /**
     * 锁服务实例。**首次访问时**才从 Spring 容器解析。
     *
     * 之前用 `lateinit var + init{}` 块在 `object` 加载时就调用 [SpringKit.getBeansOfType]——
     * 但 [SpringKit.applicationContext] 由 [io.kudos.context.spring.SpringContextInitializer]
     * 在 Spring 启动期注入。先于 [SpringContextInitializer] 被加载的代码（如其它 `object`
     * 的 companion init）会触发 `error()`。改为 `by lazy { ... }` 后，
     * - 单线程加载安全（lazy 默认 SYNCHRONIZED）
     * - Spring 上下文必须先就绪、再有第一个 `lockExecute` 调用
     */
    private val LOCK_SERVICE: ILockProvider<out Lock> by lazy {
        val beansOfType = SpringKit.getBeansOfType<ILockProvider<*>>()
        if (beansOfType.isEmpty()) {
            NormalLockService()
        } else {
            beansOfType.values.minBy { it.order() }
        }
    }

    /**
     * 在 [second] 秒窗口内尝试获取租约式锁；若 **未能** 取得（即 [ILockProvider.tryLock] 为 false），返回 `true`。
     * 语义等价于「当前 key 已被占用 / 本线程未拿到锁」，与 `tryLock` 布尔值取反。
     */
    @Deprecated(
        message = "命名反直觉（hasKeyLock 实际返回 !tryLock）。请直接用 lockProvider.tryLock 并自行取反。",
        replaceWith = ReplaceWith("!LockTool.lockProvider.tryLock(lockKey, second)")
    )
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
}
