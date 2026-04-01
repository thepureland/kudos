package io.kudos.context.core

/**
 * Kudos的上下文持有者
 *
 * 使用 InheritableThreadLocal 存储线程上下文，支持父子线程间传递。
 * 注意：在使用线程池的场景下，必须在请求/任务结束后调用 clear() 方法清理上下文，
 * 避免内存泄漏。
 *
 * @author K
 * @since 1.0.0
 */
object KudosContextHolder {

    private val contextThreadLocal = InheritableThreadLocal<KudosContext>()

    /**
     * 返回当前线程关联的 KudosContext。
     * 若当前线程尚未绑定上下文，会 **新建** [KudosContext] 并写入 ThreadLocal（不会返回 null）。
     * 若需区分「未初始化」与「已存在」，请使用 [getOrNull]。
     */
    fun get(): KudosContext {
        val kudosContext = contextThreadLocal.get() ?: KudosContext()
        contextThreadLocal.set(kudosContext)
        return kudosContext
    }

    /**
     * 返回当前线程已绑定的上下文；若尚未 [set]，返回 null（**不会**自动创建）。
     */
    fun getOrNull(): KudosContext? = contextThreadLocal.get()

    /**
     * 设置当前线程的KudosContext
     *
     * @param context Kudos上下文对象
     * @author K
     * @since 1.0.0
     */
    fun set(context: KudosContext) {
        contextThreadLocal.set(context)
    }

    /**
     * 清除当前线程的KudosContext
     * 
     * 建议在请求/任务结束时调用此方法，避免线程池复用线程时造成上下文污染和内存泄漏。
     * 
     * @author K
     * @since 1.0.0
     */
    fun clear() {
        contextThreadLocal.remove()
    }

}