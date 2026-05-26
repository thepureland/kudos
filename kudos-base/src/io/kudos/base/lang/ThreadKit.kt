package io.kudos.base.lang

import io.kudos.base.logger.LogFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Thread-related utility.
 *
 * @author K
 * @since 1.0.0
 */
object ThreadKit {

    private val LOG = LogFactory.getLog(ThreadKit::class)

    /**
     * Sleep the current thread for the specified number of milliseconds, ignoring InterruptedException.
     *
     * @param millis number of milliseconds to sleep
     * @author K
     * @since 1.0.0
     */
    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            LOG.error(e)
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Sleep the current thread for the specified duration, ignoring InterruptedException.
     *
     * @param duration the sleep duration value
     * @param unit the sleep time unit
     * @author K
     * @since 1.0.0
     */
    fun sleep(duration: Long, unit: TimeUnit) {
        try {
            Thread.sleep(unit.toMillis(duration))
        } catch (e: InterruptedException) {
            LOG.error(e)
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Graceful Shutdown implementation following the ExecutorService JavaDoc sample code. First calls shutdown to stop accepting new tasks and attempt to complete all existing tasks.
     * On timeout, calls shutdownNow to cancel tasks pending in the workQueue and interrupt all blocking calls. If it still times out, force-exits.
     * Additionally handles the case where the thread itself is interrupted while shutting down.
     *
     * @param pool the thread pool
     * @param shutdownTimeout shutdown timeout
     * @param shutdownNowTimeout shutdownNow timeout
     * @param timeUnit the time unit
     * @author K
     * @since 1.0.0
     */
    fun gracefulShutdown(pool: ExecutorService, shutdownTimeout: Int, shutdownNowTimeout: Int, timeUnit: TimeUnit) {
        pool.shutdown() // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(shutdownTimeout.toLong(), timeUnit)) {
                pool.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(shutdownNowTimeout.toLong(), timeUnit)) {
                    LOG.warn("Thread pool did not terminate!")
                }
            }
        } catch (ie: InterruptedException) {
            LOG.error(ie)
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Calls shutdownNow directly, controlled by a timeout. Cancels tasks pending in the workQueue and interrupts all blocking calls.
     *
     * @param pool the thread pool
     * @param timeout timeout
     * @param timeUnit the time unit
     * @author K
     * @since 1.0.0
     */
    fun normalShutdown(pool: ExecutorService, timeout: Int, timeUnit: TimeUnit) {
        try {
            pool.shutdownNow()
            if (!pool.awaitTermination(timeout.toLong(), timeUnit)) {
                LOG.warn("Thread pool did not terminate!")
            }
        } catch (ie: InterruptedException) {
            LOG.error(ie)
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Print the call stack to the log when it does not include the specified class name.
     * This method can be used for tracing, for example to track whether a resource has been closed.
     * Only effective when the log level is DEBUG.
     *
     * @param clazz the class
     * @author K
     * @since 1.0.0
     */
    fun printStackTraceOnNotCallByClass(clazz: KClass<*>) {
        if (!LOG.isDebugEnabled()) return
        val stackTrace = getStackTrace()
        val found = stackTrace.any { it.className == clazz.java.name }
        if (!found) {
            LOG.warn("The method stack does not contain the specified class: $clazz")
            stackTrace.forEach { LOG.warn(it.toString()) }
        }
    }

    /**
     * Print the call stack to the log without throwing an exception.
     * Only effective when the log level is DEBUG.
     *
     * @author K
     * @since 1.0.0
     */
    fun printStackTrace() {
        if (!LOG.isDebugEnabled()) return
        getStackTrace().forEach { LOG.debug(it.toString()) }
    }

    /**
     * Get the call stack.
     *
     * @return the call stack
     * @author K
     * @since 1.0.0
     */
    fun getStackTrace(): Array<StackTraceElement> = Thread.currentThread().stackTrace

}
