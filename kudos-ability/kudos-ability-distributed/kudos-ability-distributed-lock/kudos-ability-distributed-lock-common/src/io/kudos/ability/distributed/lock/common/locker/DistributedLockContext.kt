package io.kudos.ability.distributed.lock.common.locker

class DistributedLockContext {
    private val contextParam: ThreadLocal<IDistributedLockCallback?> =
        object : InheritableThreadLocal<IDistributedLockCallback?>() {
            override fun childValue(parentValue: IDistributedLockCallback?): IDistributedLockCallback? {
                return null
            }
        }

    companion object {
        private val self = DistributedLockContext()

        fun set(param: IDistributedLockCallback?) {
            self.contextParam.set(param)
        }

        fun get(): IDistributedLockCallback? {
            return self.contextParam.get()
        }
    }
}
