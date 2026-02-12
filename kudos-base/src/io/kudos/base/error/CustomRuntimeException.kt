package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.logger.LogFactory
import java.text.MessageFormat
import kotlin.math.min

/**
 * 自定义运行时异常基类
 * 
 * 扩展RuntimeException，提供更丰富的异常处理功能，包括消息格式化、堆栈跟踪控制和日志记录。
 * 
 * 核心功能：
 * 1. 消息格式化：支持MessageFormat格式的消息，支持参数替换
 * 2. 堆栈控制：支持根据错误码配置决定是否输出完整堆栈
 * 3. 日志记录：自动记录异常日志，支持控制是否记录
 * 4. 错误码支持：支持使用IErrorCodeEnum错误码枚举
 * 
 * 消息格式化：
 * - 支持MessageFormat格式，如"用户{0}不存在"
 * - 支持可变参数，自动替换占位符
 * - 如果消息为空，直接使用原消息
 * 
 * 堆栈控制：
 * - 根据errorCode.printAllStackTrace决定是否输出完整堆栈
 * - 精简模式：只保留前5行堆栈信息
 * - 完整模式：输出所有堆栈信息
 * - 使用@Synchronized确保线程安全
 * 
 * 日志记录：
 * - 默认自动记录异常日志
 * - 支持通过构造函数参数控制是否记录日志
 * - 记录异常信息和堆栈跟踪
 * 
 * 使用场景：
 * - 业务异常的统一处理
 * - 需要格式化错误消息的场景
 * - 需要控制堆栈输出的场景
 * - 需要自动日志记录的异常
 * 
 * 注意事项：
 * - 这是一个open类，可以被继承
 * - 消息格式化使用MessageFormat，注意参数顺序
 * - 堆栈精简可能丢失重要信息，需谨慎使用
 * 
 * @since 1.0.0
 */
open class CustomRuntimeException : RuntimeException {

    override var message: String? = null
        protected set

    protected constructor()

    constructor(message: String, vararg args: Any?) {
        resolveException(message, *args)
    }

    constructor(cause: Throwable) : this(cause, cause.message ?: cause.javaClass.name)

    constructor(cause: Throwable, message: String, vararg args: Any?) {
        resolveCauseException(cause, message, *args)
    }

    constructor(cause: Throwable, message: String, log: Boolean, vararg args: Any?) {
        resolveCauseException(cause, log, message, *args)
    }

    protected fun resolveException(errorCode: IErrorCodeEnum) {
        fillCustomStackTrace(errorCode)
        handleMessageWithoutLog(errorCode.trans)
        log.error(this)
    }

    protected fun resolveException(errorCode: IErrorCodeEnum, vararg args: Any?) {
        fillCustomStackTrace(errorCode)
        handleMessageWithoutLog(errorCode.trans, args)
        log.error(this)
    }

    protected fun resolveException(message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        log.error(this)
    }

    /**
     * 填充自定义堆栈跟踪信息
     * 
     * 根据错误码配置决定是否输出完整的堆栈跟踪信息。
     * 
     * 工作流程：
     * 1. 检查配置：如果errorCode.printAllStackTrace为true，填充完整堆栈
     * 2. 精简模式：如果为false，只保留前5行堆栈信息
     * 3. 设置堆栈：将处理后的堆栈信息设置到异常对象
     * 
     * 堆栈精简：
     * - 只保留前5行堆栈信息（或实际堆栈大小，取较小值）
     * - 减少日志输出量，提高可读性
     * - 保留最关键的调用栈信息
     * 
     * 线程安全：
     * - 使用@Synchronized确保多线程环境下的线程安全
     * - 避免并发修改堆栈信息导致的问题
     * 
     * 使用场景：
     * - 某些错误不需要完整的堆栈信息
     * - 减少日志文件大小
     * - 提高日志可读性
     * 
     * 注意事项：
     * - 精简模式只保留前5行，可能丢失重要信息
     * - 完整模式会输出所有堆栈信息，可能很长
     * - 配置由errorCode决定，不能动态修改
     * 
     * @param errorCode 错误码枚举，包含堆栈输出配置
     * @return 当前异常对象
     */
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

    protected fun resolveCauseException(cause: Throwable, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        log.error(cause, this.message)
    }

    protected fun resolveCauseException(cause: Throwable, shouldLog: Boolean, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        if (shouldLog) {
            log.error(cause, this.message)
        }
    }

    protected fun handleMessageWithoutLog(pattern: String, vararg args: Any?) {
        if (pattern.isNotBlank()) {
            this.message = MessageFormat.format(pattern, *args)
        } else {
            this.message = pattern
        }
    }

    private val log = LogFactory.getLog(this)

}
