package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.logger.LogFactory
import java.text.MessageFormat
import kotlin.math.min

/**
 * 自定义运行时异常
 *
 * @author K
 * @since 1.0.0
 */
open class CustomRuntimeException : RuntimeException {

    override var message: String? = null
        protected set

    protected constructor()

    constructor(message: String, vararg args: Any?) {
        resolveException(message, *args)
    }

    protected fun resolveException(errorCode: IErrorCodeEnum) {
        fillCustomStackTrace(errorCode)
        handleMessageWithoutLog(errorCode.trans)
        LOG.error(this)
    }

    protected fun resolveException(errorCode: IErrorCodeEnum, vararg args: Any?) {
        fillCustomStackTrace(errorCode)
        handleMessageWithoutLog(errorCode.trans, args)
        LOG.error(this)
    }

    protected fun resolveException(message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        LOG.error(this)
    }

    @Synchronized
    protected fun fillCustomStackTrace(errorCode: IErrorCodeEnum): Throwable? {
        if (errorCode.printAllStackTrace) {
            return fillInStackTrace()
        } else {
            //精简输出日志，如果error定义不输出全部堆栈信息
            val stackTrace = getStackTrace()
            val maxLines = min(5, stackTrace.size)
            val newStackTrace = arrayOfNulls<StackTraceElement>(maxLines)
            for (i in 0..<maxLines) {
                newStackTrace[i] = stackTrace[i]
            }
            setStackTrace(newStackTrace)
            return this
        }
    }


    constructor(ex: Throwable) : this(ex, ex.message!!)

    constructor(cause: Throwable, message: String, vararg args: Any?) {
        resolveCauseException(cause, message, *args)
    }

    protected fun resolveCauseException(cause: Throwable, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        LOG.error(cause, this.message)
    }

    constructor(cause: Throwable, log: Boolean, message: String, vararg args: Any?) {
        resolveCauseException(cause, log, message, *args)
    }

    protected fun resolveCauseException(cause: Throwable, log: Boolean, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        if (log) {
            LOG.error(cause, this.message)
        }
    }

    protected fun handleMessageWithoutLog(pattern: String, vararg args: Any?) {
        if (pattern.isNotBlank()) {
            this.message = MessageFormat.format(pattern, *args)
        } else {
            this.message = pattern
        }
    }

    private val LOG = LogFactory.getLog(this)

}
