package io.kudos.context.core

/**
 * Kudos的上下文持有者
 *
 * @since K
 * @since 1.0.0
 */
object KudosContextHolder {

    private val contextThreadLocal = InheritableThreadLocal<KudosContext>()

    init {
        contextThreadLocal.set(KudosContext())
    }

    /**
     * 返回当前线程关联的KudosContext
     *
     * @return Kudos上下文对象
     * @since K
     * @since 1.0.0
     */
    fun get(): KudosContext {
        val kudosContext = contextThreadLocal.get() ?: KudosContext()
        contextThreadLocal.set(kudosContext)
        return kudosContext
    }

    fun set(context: KudosContext) {
        contextThreadLocal.set(context)
    }

}