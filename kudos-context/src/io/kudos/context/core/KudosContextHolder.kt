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
     * 返回当前线程关联的KudosContext
     * 如果当前线程没有上下文，会创建一个新的并设置到 ThreadLocal 中
     *
     * @return Kudos上下文对象
     * @author K
     * @since 1.0.0
     */
    fun get(): KudosContext {
        val kudosContext = contextThreadLocal.get() ?: KudosContext()
        contextThreadLocal.set(kudosContext)
        return kudosContext
    }

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