package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.context.kit.SpringKit
import java.util.concurrent.locks.Lock
import java.util.function.Supplier

/**
 * Facade utility for distributed locks.
 *
 * Resolves the highest-priority [ILockProvider] implementation from the Spring container (falling back to the in-process [NormalLockService] when none exists),
 * and offers both Runnable and Supplier overloads for the common "execute a block of code under a lock" pattern, so callers don't have to write
 * the tryLock/finally/unlock template themselves.
 *
 * @author K
 * @since 1.0.0
 */
object LockTool {

    /**
     * Lock service instance. Resolved from the Spring container only **on first access**.
     *
     * Previously a `lateinit var + init{}` block called [SpringKit.getBeansOfType] at `object` load time —
     * but [SpringKit.applicationContext] is injected by [io.kudos.context.spring.SpringContextInitializer]
     * during Spring startup. Code loaded before [SpringContextInitializer] (such as the companion init of another `object`)
     * would trigger `error()`. Switching to `by lazy { ... }` ensures:
     * - Safe single-threaded initialization (lazy defaults to SYNCHRONIZED)
     * - The Spring context must be ready before the first `lockExecute` call
     */
    private val LOCK_SERVICE: ILockProvider<out Lock> by lazy {
        SpringKit.getBeansOfType<ILockProvider<*>>().values.minByOrNull { it.order() } ?: NormalLockService()
    }

    /**
     * Attempts to acquire a lease-style lock within a [second]-second window; returns `true` **only when** the acquisition **fails**
     * (i.e. [ILockProvider.tryLock] returns false). The semantics are equivalent to "the current key is held / the current thread did not acquire the lock", which is the negation of `tryLock`.
     */
    @Deprecated(
        message = "Counter-intuitive name (hasKeyLock actually returns !tryLock). Use lockProvider.tryLock directly and negate it yourself.",
        replaceWith = ReplaceWith("!LockTool.lockProvider.tryLock(lockKey, second)")
    )
    fun hasKeyLock(lockKey: String, second: Int): Boolean = !LOCK_SERVICE.tryLock(lockKey, second)

    /**
     * Acquires a lock on [lockKey] and executes [supplier], with a default lease of 90 seconds (to prevent deadlock after a crash).
     *
     * @param T the return type
     * @param lockKey the business key for the lock
     * @param supplier the logic to execute
     * @param errorCode the error code thrown if lock acquisition fails
     * @return the return value of [supplier]
     * @author K
     * @since 1.0.0
     */
    fun <T> lockExecute(lockKey: String, supplier: Supplier<T?>, errorCode: IErrorCodeEnum): T? =
        lockExecute<T?>(lockKey, supplier, 90, errorCode)

    /**
     * Acquires a lock on [lockKey] and executes [supplier], with a lease of [second] seconds.
     *
     * @param T the return type
     * @param lockKey the business key for the lock
     * @param supplier the logic to execute
     * @param second lease seconds; automatically released on timeout to prevent deadlock after a crash
     * @param errorCode the error code thrown if lock acquisition fails
     * @return the return value of [supplier]
     * @author K
     * @since 1.0.0
     */
    fun <T> lockExecute(lockKey: String, supplier: Supplier<T?>, second: Int, errorCode: IErrorCodeEnum): T? =
        lockProvider.lockExecute(lockKey, supplier, second, errorCode)

    /**
     * Acquires a lock on [lockKey] and executes [runnable], with a default lease of 90 seconds.
     *
     * @param lockKey the business key for the lock
     * @param runnable the logic to execute
     * @param errorCode the error code thrown if lock acquisition fails
     * @author K
     * @since 1.0.0
     */
    fun lockExecute(lockKey: String, runnable: Runnable, errorCode: IErrorCodeEnum) {
        lockExecute(lockKey, runnable, 90, errorCode)
    }

    /**
     * Acquires a lock on [lockKey] and executes [runnable], with a lease of [second] seconds.
     *
     * @param lockKey the business key for the lock
     * @param runnable the logic to execute
     * @param second lease seconds; automatically released on timeout to prevent deadlock after a crash
     * @param errorCode the error code thrown if lock acquisition fails
     * @author K
     * @since 1.0.0
     */
    fun lockExecute(lockKey: String, runnable: Runnable, second: Int, errorCode: IErrorCodeEnum) {
        lockProvider.lockExecute(lockKey, runnable, second, errorCode)
    }

    /** The currently active lock provider, equivalent to [LOCK_SERVICE]; exposed to business code for more fine-grained lock operations. */
    val lockProvider: ILockProvider<out Lock>
        get() = LOCK_SERVICE
}
