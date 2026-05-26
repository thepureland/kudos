package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.logger.LogFactory
import java.text.MessageFormat
import kotlin.math.min

/**
 * Base class for custom runtime exceptions.
 *
 * Extends RuntimeException with richer exception handling, including message formatting,
 * stack-trace control, and logging.
 *
 * Core features:
 * 1. Message formatting: supports MessageFormat-style messages with parameter substitution.
 * 2. Stack control: optionally outputs the full stack based on the error code configuration.
 * 3. Logging: automatically records exception logs, with the option to disable.
 * 4. Error-code support: works with the IErrorCodeEnum error-code enum.
 *
 * Message formatting:
 * - Supports MessageFormat templates such as "User {0} does not exist".
 * - Supports vararg arguments that automatically replace placeholders.
 * - If the message is blank, uses the original message as-is.
 *
 * Stack control:
 * - The printAllStackTrace parameter decides whether to output the full stack.
 * - Compact mode: keeps the first 20 frames of the JVM-captured stack (the old version kept 5,
 *   which often cut off critical business frames).
 * - Full mode: outputs the entire stack.
 * - Uses @Synchronized to guarantee thread safety.
 *
 * Logging:
 * - Records exception logs automatically by default.
 * - Logging can be toggled via a constructor parameter.
 * - Records both the exception message and the stack trace.
 *
 * Use cases:
 * - Unified handling of business exceptions.
 * - Scenarios that require formatted error messages.
 * - Scenarios that need to control stack output.
 * - Exceptions that should be logged automatically.
 *
 * Notes:
 * - This is an open class and can be inherited.
 * - Message formatting uses MessageFormat; mind the parameter order.
 * - In compact mode, when business frames exceed 20 the tail is still lost; use full mode for critical scenarios.
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
     * Minimal error-code-based exception handling: compact stack + errorCode.displayText as the
     * message + ERROR-level log.
     *
     * @param errorCode error-code enum
     * @author K
     * @since 1.0.0
     */
    protected fun resolveException(errorCode: IErrorCodeEnum) {
        fillCustomStackTrace(errorCode)
        handleMessageWithoutLog(errorCode.displayText)
        log.error(this)
    }

    /**
     * Error-code variant with configurable stack depth: when [printAllStackTrace] is true the full
     * stack is preserved; when false the stack is truncated to [MAX_STACK_LINES]
     * (see the design notes on [fillCustomStackTrace]).
     *
     * @param errorCode error-code enum
     * @param printAllStackTrace whether to keep the full stack
     * @param args MessageFormat arguments
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
     * Custom-message variant (no error code): keeps the full stack, formats the message, and logs at ERROR.
     *
     * @param message MessageFormat template
     * @param args template arguments
     * @author K
     * @since 1.0.0
     */
    protected fun resolveException(message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        log.error(this)
    }

    /**
     * Fills the custom stack trace.
     *
     * - printAllStackTrace=true: calls fillInStackTrace again at the current location, preserving the full stack.
     * - printAllStackTrace=false (compact mode): keeps the first [MAX_STACK_LINES] frames of the JVM-captured stack.
     *
     * Design notes:
     * - The old implementation hard-coded a cap of 5 frames. In production, if the business call chain is
     *   slightly deep (DAO -> Service -> Controller -> AOP aspect -> gateway filter ...), the key upstream
     *   callers were often truncated, making issues hard to locate.
     * - Raising the cap to 20 is an empirical value that covers a typical Spring MVC chain while still
     *   avoiding log explosion.
     *
     * Thread safety: uses @Synchronized to avoid concurrent modification of the stack array.
     *
     * @param errorCode error-code enum (kept for subclass use; not read by this implementation)
     * @param printAllStackTrace true to skip trimming
     * @return the current exception
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
     * Wraps an external exception as cause: logs the original cause together with the formatted self-message,
     * so the original stack can be reconstructed during troubleshooting.
     *
     * @param cause underlying exception
     * @param message MessageFormat template
     * @param args template arguments
     * @author K
     * @since 1.0.0
     */
    protected fun resolveCauseException(cause: Throwable, message: String, vararg args: Any?) {
        fillInStackTrace()
        handleMessageWithoutLog(message, *args)
        log.error(cause, this.message)
    }

    /**
     * Wraps an external exception with optional logging: `shouldLog=false` is for "expected business
     * exceptions that can be handled", preventing log noise from drowning out genuine ERRORs.
     *
     * @param cause underlying exception
     * @param shouldLog whether to actually write the log
     * @param message MessageFormat template
     * @param args template arguments
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
     * Fills the message only and does not write a log -- logging is left to the caller in
     * [resolveException] / [resolveCauseException] to avoid duplicate entries.
     *
     * An empty pattern is treated as the literal message (skipping MessageFormat to avoid issues with
     * special characters such as `{0}`).
     *
     * @param pattern MessageFormat template; treated as a literal message when blank
     * @param args template arguments
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
        /** Upper bound on retained stack frames in compact mode. */
        private const val MAX_STACK_LINES = 20
    }

}
