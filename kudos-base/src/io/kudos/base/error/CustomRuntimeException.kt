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
 * - 根据 printAllStackTrace 参数决定是否输出完整堆栈
 * - 精简模式：保留 JVM 自动捕获堆栈的前 20 帧（旧版为 5 帧，常切掉关键业务栈）
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
 * - 精简模式下若业务栈帧数超过 20 仍会丢失尾部信息，关键场景请使用完整模式
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

    /**
     * 基于错误码的最简异常处理：精简栈 + 用 errorCode.displayText 作消息 + ERROR 日志。
     *
     * @param errorCode 错误码枚举
     * @author K
     * @since 1.0.0
     */
    protected fun resolveException(errorCode: IErrorCodeEnum) {
        fillCustomStackTrace(errorCode)
        handleMessageWithoutLog(errorCode.displayText)
        log.error(this)
    }

    /**
     * 错误码 + 可控堆栈深度版本：[printAllStackTrace]=true 时保留完整栈，
     * false 时按 [MAX_STACK_LINES] 截断（参考 [fillCustomStackTrace] 的设计说明）。
     *
     * @param errorCode 错误码枚举
     * @param printAllStackTrace 是否完整堆栈
     * @param args MessageFormat 用的格式化参数
     * @author K
     * @since 1.0.0
     */
    protected fun resolveException(
        errorCode: IErrorCodeEnum,
        printAllStackTrace: Boolean,
        vararg args: Any?
    ) {
        fillCustomStackTrace(errorCode, printAllStackTrace)
        handleMessageWithoutLog(errorCode.displayText, *args)
        log.error(this)
    }

    /**
     * 自定义消息版本（不走错误码）：保留完整栈 + 格式化消息 + ERROR 日志。
     *
     * @param message MessageFormat 模板
     * @param args 模板参数
     * @author K
     * @since 1.0.0
     */
    protected fun resolveException(message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        log.error(this)
    }

    /**
     * 填充自定义堆栈跟踪信息
     *
     * - printAllStackTrace=true：在当前位置重新调用 fillInStackTrace，保留完整堆栈
     * - printAllStackTrace=false（精简模式）：保留 JVM 自动捕获的栈的前 [MAX_STACK_LINES] 帧
     *
     * 设计说明：
     * - 旧实现固定截到前 5 帧。生产中如果业务调用链稍深（DAO → Service → Controller →
     *   AOP 切面 → 网关过滤器…），关键的上层调用方常被切掉，难以定位。
     * - 提升为 20 是经验值，覆盖典型 Spring MVC 全链路而仍避免日志爆炸。
     *
     * 线程安全：使用 @Synchronized 避免并发修改堆栈数组。
     *
     * @param errorCode 错误码枚举（保留供子类使用，本实现未读取）
     * @param printAllStackTrace true 时不裁剪堆栈
     * @return 当前异常对象
     */
    @Synchronized
    protected fun fillCustomStackTrace(
        errorCode: IErrorCodeEnum,
        printAllStackTrace: Boolean = false
    ): Throwable? {
        if (printAllStackTrace) {
            return fillInStackTrace()
        }
        val stackTrace = getStackTrace()
        if (stackTrace.size <= MAX_STACK_LINES) {
            return this
        }
        setStackTrace(stackTrace.copyOfRange(0, MAX_STACK_LINES))
        return this
    }

    /**
     * 包装外部异常作为 cause：日志带原 cause + 自身格式化消息，便于排错时还原原始堆栈。
     *
     * @param cause 底层异常
     * @param message MessageFormat 模板
     * @param args 模板参数
     * @author K
     * @since 1.0.0
     */
    protected fun resolveCauseException(cause: Throwable, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        log.error(cause, this.message)
    }

    /**
     * 包装外部异常并可选择是否打日志——`shouldLog=false` 用于"业务上能处理的可预期异常"，
     * 避免日志噪声把真正的 ERROR 淹没。
     *
     * @param cause 底层异常
     * @param shouldLog 是否真的写日志
     * @param message MessageFormat 模板
     * @param args 模板参数
     * @author K
     * @since 1.0.0
     */
    protected fun resolveCauseException(cause: Throwable, shouldLog: Boolean, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        if (shouldLog) {
            log.error(cause, this.message)
        }
    }

    /**
     * 只填 message，不写日志——日志的事交给外层 [resolveException] / [resolveCauseException]
     * 决定，避免重复打。
     *
     * 空 pattern 直接当 message（不走 MessageFormat 避免 `{0}` 之类的特殊字符意外破坏）。
     *
     * @param pattern MessageFormat 模板；空白时按字面 message 处理
     * @param args 模板参数
     * @author K
     * @since 1.0.0
     */
    protected fun handleMessageWithoutLog(pattern: String, vararg args: Any?) {
        if (pattern.isNotBlank()) {
            this.message = MessageFormat.format(pattern, *args)
        } else {
            this.message = pattern
        }
    }

    private val log = LogFactory.getLog(this::class)

    companion object {
        /** 精简堆栈模式下保留的栈帧数上限 */
        private const val MAX_STACK_LINES = 20
    }

}
