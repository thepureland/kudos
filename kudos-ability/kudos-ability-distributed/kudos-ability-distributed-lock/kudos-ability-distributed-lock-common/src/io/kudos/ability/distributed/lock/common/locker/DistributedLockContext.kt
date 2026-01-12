package io.kudos.ability.distributed.lock.common.locker

/**
 * 分布式锁上下文
 * 
 * 使用 InheritableThreadLocal 存储线程上下文，支持父子线程间传递。
 * 注意：在使用线程池的场景下，必须在请求/任务结束后调用 clear() 方法清理上下文，
 * 避免内存泄漏。
 *
 * @author K
 * @since 1.0.0
 */
class DistributedLockContext {
    private val contextParam: ThreadLocal<IDistributedLockCallback?> =
        object : InheritableThreadLocal<IDistributedLockCallback?>() {
            override fun childValue(parentValue: IDistributedLockCallback?): IDistributedLockCallback? {
                // 返回 null 表示子线程不继承父线程的值
                return null
            }
        }

    companion object {
        private val self = DistributedLockContext()

        /**
         * 设置当前线程的分布式锁回调
         *
         * @param param 分布式锁回调接口
         * @author K
         * @since 1.0.0
         */
        fun set(param: IDistributedLockCallback?) {
            self.contextParam.set(param)
        }

        /**
         * 获取当前线程的分布式锁回调
         *
         * @return 分布式锁回调接口
         * @author K
         * @since 1.0.0
         */
        fun get(): IDistributedLockCallback? {
            return self.contextParam.get()
        }

        /**
         * 清除当前线程的分布式锁上下文
         * 
         * 建议在请求/任务结束时调用此方法，避免线程池复用线程时造成上下文污染和内存泄漏。
         * 
         * @author K
         * @since 1.0.0
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
