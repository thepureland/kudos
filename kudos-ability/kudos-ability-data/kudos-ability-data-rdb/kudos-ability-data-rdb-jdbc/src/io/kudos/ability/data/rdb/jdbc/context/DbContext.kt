package io.kudos.ability.data.rdb.jdbc.context

/**
 * 数据库上下文
 * 
 * 使用 InheritableThreadLocal 存储线程上下文，支持父子线程间传递。
 * 注意：在使用线程池的场景下，必须在请求/任务结束后调用 clear() 方法清理上下文，
 * 避免内存泄漏。
 *
 * @author K
 * @since 1.0.0
 */
class DbContext {
    private val contextParam: ThreadLocal<DbParam?> = object : InheritableThreadLocal<DbParam?>() {
        override fun childValue(parentValue: DbParam?): DbParam? {
            // 返回 null 表示子线程不继承父线程的值
            return null
        }
    }

    companion object {
        private val self = DbContext()

        /**
         * 设置当前线程的数据库上下文参数
         *
         * @param param 数据库参数
         * @author K
         * @since 1.0.0
         */
        fun set(param: DbParam?) {
            self.contextParam.set(param)
        }

        /**
         * 获取当前线程的数据库上下文参数
         * 如果当前线程没有上下文，会创建一个新的并设置到 ThreadLocal 中
         *
         * @return 数据库参数
         * @author K
         * @since 1.0.0
         */
        fun get(): DbParam {
            if (self.contextParam.get() == null) {
                self.contextParam.set(DbParam())
            }
            return requireNotNull(self.contextParam.get()) { "contextParam is null" }
        }

        /**
         * 清除当前线程的数据库上下文
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
