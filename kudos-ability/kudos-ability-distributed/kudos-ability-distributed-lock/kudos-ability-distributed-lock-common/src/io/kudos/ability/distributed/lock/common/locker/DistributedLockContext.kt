package io.kudos.ability.distributed.lock.common.locker

/**
 * Distributed lock context.
 *
 * Uses InheritableThreadLocal to store thread context, supporting propagation from parent to
 * child threads.
 * Note: in thread-pool scenarios, clear() must be called at the end of every request/task to
 * avoid memory leaks.
 *
 * @author K
 * @since 1.0.0
 */
class DistributedLockContext {
    private val contextParam: ThreadLocal<IDistributedLockCallback?> =
        object : InheritableThreadLocal<IDistributedLockCallback?>() {
            // Returning null means the child thread does not inherit the parent's value.
            override fun childValue(parentValue: IDistributedLockCallback?): IDistributedLockCallback? = null
        }

    companion object {
        private val self = DistributedLockContext()

        /**
         * Set the distributed-lock callback for the current thread.
         *
         * @param param the distributed-lock callback
         * @author K
         * @since 1.0.0
         */
        fun set(param: IDistributedLockCallback?) {
            self.contextParam.set(param)
        }

        /**
         * Get the distributed-lock callback for the current thread.
         *
         * @return the distributed-lock callback
         * @author K
         * @since 1.0.0
         */
        fun get(): IDistributedLockCallback? = self.contextParam.get()

        /**
         * Clear the distributed-lock context for the current thread.
         *
         * Call this at the end of each request/task to avoid context pollution and memory leaks
         * when threads are reused by a thread pool.
         *
         * @author K
         * @since 1.0.0
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
