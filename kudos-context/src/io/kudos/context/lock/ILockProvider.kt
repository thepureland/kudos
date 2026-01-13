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
     * 上锁执行，执行完毕后自动释放锁
     * 
     * 尝试获取锁，如果获取成功则执行supplier，执行完成后自动释放锁。
     * 
     * 工作流程：
     * 1. 尝试获取锁：调用tryLock尝试获取锁，等待时间为sec秒
     * 2. 获取成功：执行supplier.get()，在finally块中释放锁
     * 3. 获取失败：如果提供了errorCode，抛出ServiceException；否则返回null
     * 
     * 资源管理：
     * - 使用try-finally确保锁一定会被释放
     * - 即使supplier抛出异常，锁也会被正确释放
     * 
     * 异常处理：
     * - 如果获取锁失败且提供了errorCode，会抛出ServiceException
     * - 如果获取锁失败且未提供errorCode，返回null
     * - supplier抛出的异常会向上传播
     * 
     * 使用场景：
     * - 需要确保代码块在锁保护下执行
     * - 希望自动管理锁的获取和释放
     * - 需要处理获取锁失败的情况
     * 
     * 注意事项：
     * - 锁的获取有超时时间（sec秒）
     * - 如果超时未获取到锁，会根据errorCode决定是否抛出异常
     * - supplier的返回值会被返回
     * 
     * @param lockKey 锁的key
     * @param supplier 需要在锁保护下执行的代码，返回T类型的结果
     * @param sec 获取锁的等待时间（秒）
     * @param errorCode 获取锁失败时的错误码，如果为null则不抛出异常
     * @return supplier的返回值，如果获取锁失败且errorCode为null则返回null
     * @throws ServiceException 如果获取锁失败且errorCode不为null
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
     * 上锁执行，执行完毕后自动释放锁
     * 
     * 尝试获取锁，如果获取成功则执行runnable，执行完成后自动释放锁。
     * 
     * 工作流程：
     * 1. 尝试获取锁：调用tryLock尝试获取锁，等待时间为sec秒
     * 2. 获取成功：执行runnable.run()，在finally块中释放锁
     * 3. 获取失败：如果提供了errorCode，抛出ServiceException
     * 
     * 资源管理：
     * - 使用try-finally确保锁一定会被释放
     * - 即使runnable抛出异常，锁也会被正确释放
     * 
     * 异常处理：
     * - 如果获取锁失败且提供了errorCode，会抛出ServiceException
     * - 如果获取锁失败且未提供errorCode，静默返回
     * - runnable抛出的异常会向上传播
     * 
     * 使用场景：
     * - 需要确保代码块在锁保护下执行
     * - 希望自动管理锁的获取和释放
     * - 不需要返回值的场景
     * 
     * 注意事项：
     * - 锁的获取有超时时间（sec秒）
     * - 如果超时未获取到锁，会根据errorCode决定是否抛出异常
     * - runnable没有返回值
     * 
     * @param lockKey 锁的key
     * @param runnable 需要在锁保护下执行的代码
     * @param sec 获取锁的等待时间（秒）
     * @param errorCode 获取锁失败时的错误码，如果为null则不抛出异常
     * @throws ServiceException 如果获取锁失败且errorCode不为null
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
