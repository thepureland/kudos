package io.kudos.base.logger

/**
 * Logger that keeps the underlying third-party logging library transparent to callers.
 *
 * @author K
 * @since 1.0.0
 */
interface ILog {

    /**
     * Logs a trace-level message.
     * @param msg the log content; supports Java string templates. If parameters are computed eagerly and performance is a concern (i.e. you want to defer computation), use a Kotlin string template instead.
     * @param args Java string template arguments
     * @author K
     * @since 1.0.0
     */
    fun trace(msg: String?, vararg args: Any?)

    /**
     * Logs a debug-level message.
     * @param msg the log content; supports Java string templates. If parameters are computed eagerly and performance is a concern (i.e. you want to defer computation), use a Kotlin string template instead.
     * @param args Java string template arguments
     * @author K
     * @since 1.0.0
     */
    fun debug(msg: String?, vararg args: Any?)

    /**
     * Logs an info-level message.
     * @param msg the log content; supports Java string templates. If parameters are computed eagerly and performance is a concern (i.e. you want to defer computation), use a Kotlin string template instead.
     * @param args Java string template arguments
     * @author K
     * @since 1.0.0
     */
    fun info(msg: String?, vararg args: Any?)

    /**
     * Logs a warning-level message.
     * @param msg the log content; supports Java string templates. If parameters are computed eagerly and performance is a concern (i.e. you want to defer computation), use a Kotlin string template instead.
     * @param args Java string template arguments
     * @author K
     * @since 1.0.0
     */
    fun warn(msg: String?, vararg args: Any?)

    /**
     * Logs an error-level message.
     * @param msg the log content; supports Java string templates. If parameters are computed eagerly and performance is a concern (i.e. you want to defer computation), use a Kotlin string template instead.
     * @param args Java string template arguments
     * @author K
     * @since 1.0.0
     */
    fun error(msg: String?, vararg args: Any?)

    /**
     * Logs an error-level message.
     * @param e the throwable
     * @param msg the log content; supports Java string templates. If parameters are computed eagerly and performance is a concern (i.e. you want to defer computation), use a Kotlin string template instead.
     * @param args Java string template arguments
     * @author K
     * @since 1.0.0
     */
    fun error(e: Throwable, msg: String?, vararg args: Any?)

    /**
     * Logs an error-level message.
     *
     * @param e the throwable
     * @author K
     * @since 1.0.0
     */
    fun error(e: Throwable)

    /**
     * Whether trace-level and higher logging is enabled.
     *
     * @return true: trace-level logging is enabled; false: it is not
     * @author K
     * @since 1.0.0
     */
    fun isTraceEnabled(): Boolean

    /**
     * Whether debug-level and higher logging is enabled.
     *
     * @return true: debug-level logging is enabled; false: it is not
     * @author K
     * @since 1.0.0
     */
    fun isDebugEnabled(): Boolean

    /**
     * Whether info-level and higher logging is enabled.
     *
     * @return true: info-level logging is enabled; false: it is not
     * @author K
     * @since 1.0.0
     */
    fun isInfoEnabled(): Boolean

    /**
     * Whether warn-level and higher logging is enabled.
     *
     * @return true: warn-level logging is enabled; false: it is not
     * @author K
     * @since 1.0.0
     */
    fun isWarnEnabled(): Boolean

    /**
     * Whether error-level logging is enabled.
     *
     * @return true: error-level logging is enabled; false: it is not
     * @author K
     * @since 1.0.0
     */
    fun isErrorEnabled(): Boolean

}
