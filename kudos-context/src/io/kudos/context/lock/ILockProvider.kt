package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.error.ServiceException
import java.util.concurrent.locks.Lock


interface ILockProvider<L : Lock> {
    /**
     * 类似锁
     * @param key
     * @return
     */
    fun lock(key: String): L?

    /**
     * 释放锁
     * @param lock
     * @param key
     */
    fun unLock(lock: Lock, key: String)

    /**
     * 解除锁
     * @param key
     */
    fun unLock(key: String)

    fun order(): Int {
        //优先级顺序，值越小顺序越大
        return 99
    }

    fun tryLock(lockKey: String, sec: Int): Boolean

    /**
     * 上锁执行，执行完毕后释放锁
     * @param lockKey
     * @param supplier
     * @param sec
     */
    fun <T> lockExecute(
        lockKey: String,
        supplier: java.util.function.Supplier<T?>,
        sec: Int,
        errorCode: IErrorCodeEnum?
    ): T? {
        val b = tryLock(lockKey, sec)
        if (b) {
            try {
                return supplier.get()
            } finally {
                unLock(lockKey)
            }
        } else {
            if (errorCode != null) {
                throw ServiceException(errorCode)
            }
        }
        return null
    }

    /**
     * 上锁执行，执行完毕后释放锁
     * @param lockKey
     * @param sec
     */
    fun lockExecute(lockKey: String, runnable: Runnable, sec: Int, errorCode: IErrorCodeEnum?) {
        val b = tryLock(lockKey, sec)
        if (b) {
            try {
                runnable.run()
            } finally {
                unLock(lockKey)
            }
        } else {
            if (errorCode != null) {
                throw ServiceException(errorCode)
            }
        }
    }

    companion object {
        const val BEAN_NAME: String = "failedLockProvider"
    }
}
