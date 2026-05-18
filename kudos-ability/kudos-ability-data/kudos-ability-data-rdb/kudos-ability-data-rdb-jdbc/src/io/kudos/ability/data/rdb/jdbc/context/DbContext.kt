package io.kudos.ability.data.rdb.jdbc.context

/**
 * 数据库路由参数的线程局部上下文。
 *
 * 用 [InheritableThreadLocal] 存当前线程的 [DbParam]，AOP 切面据此选择数据源。
 * 子线程 **不** 继承父线程的值（`childValue` 返回 null）—— 避免线程池里的子任务
 * 意外继承上一个请求的路由意图。
 *
 * 与 `KudosContextHolder` 同样的契约：使用线程池的场景**必须**在请求/任务结束时调用
 * [clear]，否则线程被复用时会带着旧的 [DbParam] 跑下一个请求，造成数据源串台。
 *
 * @author K
 * @since 1.0.0
 */
class DbContext {
    private val contextParam: ThreadLocal<DbParam?> = object : InheritableThreadLocal<DbParam?>() {
        override fun childValue(parentValue: DbParam?): DbParam? {
            // 子线程不继承父线程的值，杜绝线程池里"上一个 task 的路由意图被新 task 继承"
            return null
        }
    }

    companion object {
        private val self = DbContext()

        /**
         * 显式写入当前线程的 [DbParam]。传 `null` 等价于清空当前线程的绑定（但**不会**
         * remove 掉 ThreadLocal slot，仅置 null —— 想真清掉用 [clear]）。
         */
        fun set(param: DbParam?) {
            self.contextParam.set(param)
        }

        /**
         * 取当前线程的 [DbParam]，**没绑定时会自动新建一个空 [DbParam] 并写回 ThreadLocal**。
         *
         * 这种"取的同时偷偷创建"的语义历史遗留下来 —— 切面要求 get() 永远返回非空，方便链
         * 写 `DbContext.get().forcedDs = ...`。代价是：在不知情的代码路径里调用 get() 会
         * 顺手把一个空 DbParam 塞进当前线程的 ThreadLocal slot，线程池场景里若忘记 [clear]
         * 就是泄漏来源。
         *
         * **如果只是"看一眼当前是否有上下文"，请用 [getOrNull]。**
         */
        fun get(): DbParam {
            if (self.contextParam.get() == null) {
                self.contextParam.set(DbParam())
            }
            return requireNotNull(self.contextParam.get()) { "contextParam is null" }
        }

        /**
         * 取当前线程已绑定的 [DbParam]，未绑定时返回 `null`，**不创建任何新对象**、**不写
         * ThreadLocal**。供"只读侦测"路径使用，避免被 [get] 的副作用污染线程局部状态。
         */
        fun getOrNull(): DbParam? = self.contextParam.get()

        /**
         * 彻底清除当前线程的 [DbParam] 绑定（`ThreadLocal.remove`）。**线程池场景每次任
         * 务结束必须调用**，否则线程被复用时会带着旧 [DbParam] 跑下个任务。
         */
        fun clear() {
            self.contextParam.remove()
        }

    }
}
