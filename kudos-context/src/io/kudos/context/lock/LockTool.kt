package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.context.kit.SpringKit
import java.util.concurrent.locks.Lock
import java.util.function.Supplier

/**
 * 分布式锁的门面工具。
 *
 * 从 Spring 容器拿到优先级最高的 [ILockProvider] 实现（无实现时回退到进程内 [NormalLockService]），
 * 并对常见的"带锁执行一段代码"模式提供同步与 Supplier 两种重载，避免每个调用方都写一遍
 * tryLock/finally/unlock 模板。
 *
 * @author K
 * @since 1.0.0
 */
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
        SpringKit.getBeansOfType<ILockProvider<*>>().values.minByOrNull { it.order() } ?: NormalLockService()
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

    /**
     * 在 [lockKey] 上加锁后执行 [supplier]，默认租约 90 秒（防止崩溃后死锁）。
     *
     * @param T 返回类型
     * @param lockKey 锁的业务 key
     * @param supplier 实际要执行的逻辑
     * @param errorCode 抢锁失败时抛出的错误码
     * @return [supplier] 的返回值
     * @author K
     * @since 1.0.0
     */
    fun <T> lockExecute(lockKey: String, supplier: Supplier<T?>, errorCode: IErrorCodeEnum): T? =
        lockExecute<T?>(lockKey, supplier, 90, errorCode)

    /**
     * 在 [lockKey] 上加锁后执行 [supplier]，租约 [second] 秒。
     *
     * @param T 返回类型
     * @param lockKey 锁的业务 key
     * @param supplier 实际要执行的逻辑
     * @param second 租约秒数；超时自动释放，避免崩溃后死锁
     * @param errorCode 抢锁失败时抛出的错误码
     * @return [supplier] 的返回值
     * @author K
     * @since 1.0.0
     */
    fun <T> lockExecute(lockKey: String, supplier: Supplier<T?>, second: Int, errorCode: IErrorCodeEnum): T? =
        lockProvider.lockExecute(lockKey, supplier, second, errorCode)

    /**
     * 在 [lockKey] 上加锁后执行 [runnable]，默认租约 90 秒。
     *
     * @param lockKey 锁的业务 key
     * @param runnable 实际要执行的逻辑
     * @param errorCode 抢锁失败时抛出的错误码
     * @author K
     * @since 1.0.0
     */
    fun lockExecute(lockKey: String, runnable: Runnable, errorCode: IErrorCodeEnum) {
        lockExecute(lockKey, runnable, 90, errorCode)
    }

    /**
     * 在 [lockKey] 上加锁后执行 [runnable]，租约 [second] 秒。
     *
     * @param lockKey 锁的业务 key
     * @param runnable 实际要执行的逻辑
     * @param second 租约秒数；超时自动释放，避免崩溃后死锁
     * @param errorCode 抢锁失败时抛出的错误码
     * @author K
     * @since 1.0.0
     */
    fun lockExecute(lockKey: String, runnable: Runnable, second: Int, errorCode: IErrorCodeEnum) {
        lockProvider.lockExecute(lockKey, runnable, second, errorCode)
    }

    /** 当前生效的锁提供者，等价于 [LOCK_SERVICE]；暴露给业务代码做更细粒度的锁操作 */
    val lockProvider: ILockProvider<out Lock>
        get() = LOCK_SERVICE
}
