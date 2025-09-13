package io.kudos.ability.data.rdb.jdbc.context

class DbContext {
    private val contextParam: ThreadLocal<DbParam?> = object : InheritableThreadLocal<DbParam?>() {
        override fun childValue(parentValue: DbParam?): DbParam? {
            // 返回 null 表示子线程不继承父线程的值
            return null
        }
    }

    companion object {
        private val self = DbContext()

        fun set(param: DbParam?) {
            self.contextParam.set(param)
        }

        fun get(): DbParam {
            if (self.contextParam.get() == null) {
                self.contextParam.set(DbParam())
            }
            return self.contextParam.get()!!
        }
    }
}
