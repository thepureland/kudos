package io.kudos.context.core

/**
 * Holder of the Kudos context.
 *
 * Uses InheritableThreadLocal to store thread context, supporting propagation between parent and child threads.
 * Note: in scenarios that use thread pools, you must call clear() after a request/task completes to clean up the
 * context and avoid memory leaks.
 *
 * @author K
 * @since 1.0.0
 */
object KudosContextHolder {

    private val contextThreadLocal = InheritableThreadLocal<KudosContext>()

    /**
     * Return the KudosContext associated with the current thread.
     * If the current thread has no context bound, **create** a new [KudosContext] and write it into the ThreadLocal
     * (it will never return null). To distinguish "uninitialized" from "already present", use [getOrNull].
     */
    fun get(): KudosContext =
        contextThreadLocal.get() ?: KudosContext().also { contextThreadLocal.set(it) }

    /**
     * Return the context bound to the current thread; returns null if it has not been [set] (will **not** auto-create).
     */
    fun getOrNull(): KudosContext? = contextThreadLocal.get()

    /**
     * Set the current thread's KudosContext.
     *
     * @param context The Kudos context object
     * @author K
     * @since 1.0.0
     */
    fun set(context: KudosContext) {
        contextThreadLocal.set(context)
    }

    /**
     * Clear the current thread's KudosContext.
     *
     * It is recommended to call this method at the end of a request/task to avoid context pollution and memory leaks
     * when threads are reused by a thread pool.
     *
     * @author K
     * @since 1.0.0
     */
    fun clear() {
        contextThreadLocal.remove()
    }

}