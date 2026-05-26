package io.kudos.base.logger.slf4j

import io.kudos.base.logger.ILog
import org.slf4j.Logger
import org.slf4j.spi.LocationAwareLogger
import java.text.MessageFormat

/**
 * Logger implementation backed by SLF4J.
 *
 * @author K
 * @since 1.0.0
 */
open class Slf4jLogger : ILog {

    private var logger: LocationAwareLogger
    private val FQCN: String = this::class.java.name

    constructor(logger: Logger) {
        this.logger = logger as LocationAwareLogger
    }


    override fun trace(msg: String?, vararg args: Any?) {
        if (logger.isTraceEnabled) // Early check to avoid potentially unnecessary string-template interpolation
            logger.log(null, FQCN, LocationAwareLogger.TRACE_INT, getMsg(msg, *args), null, null)
    }

    override fun debug(msg: String?, vararg args: Any?) {
        if (logger.isDebugEnabled) // Early check to avoid potentially unnecessary string-template interpolation
            logger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, getMsg(msg, *args), null, null)
    }

    override fun info(msg: String?, vararg args: Any?) =
//        if (log.isInfoEnabled) // Unlike trace/debug, no early check here: info is normally enabled, so the
//                               // early check is typically redundant computation; the same applies to warn/error.
        logger.log(null, FQCN, LocationAwareLogger.INFO_INT, getMsg(msg, *args), null, null)

    override fun warn(msg: String?, vararg args: Any?) =
        logger.log(null, FQCN, LocationAwareLogger.WARN_INT, getMsg(msg, *args), null, null)

    override fun error(msg: String?, vararg args: Any?) =
        logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, getMsg(msg, *args), null, null)

    override fun error(e: Throwable, msg: String?, vararg args: Any?) =
        logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, getMsg(msg, *args), null, e)

    override fun error(e: Throwable) =
        logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, getMsg(e.message), null, e)

    /**
     * Performs [MessageFormat] placeholder substitution. With no arguments, skips formatting and returns the
     * original string to avoid misinterpreting characters such as `{0}`.
     *
     * @param msg the template
     * @param args the substitution arguments
     * @return the formatted message (returns null directly for null input)
     * @author K
     * @since 1.0.0
     */
    private fun getMsg(msg: String?, vararg args: Any?): String? {
        return if (msg != null && args.isNotEmpty()) {
            MessageFormat.format(msg, *args)
        } else msg
    }

    override fun isTraceEnabled(): Boolean = logger.isTraceEnabled

    override fun isDebugEnabled(): Boolean = logger.isDebugEnabled

    override fun isInfoEnabled(): Boolean = logger.isInfoEnabled

    override fun isWarnEnabled(): Boolean = logger.isWarnEnabled

    override fun isErrorEnabled(): Boolean = logger.isErrorEnabled

}