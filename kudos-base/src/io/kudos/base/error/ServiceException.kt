package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum


/**
 * Service-layer exception.
 *
 * Thrown for business-logic errors in the service layer, with support for error codes and parameterized messages.
 *
 * Core features:
 * 1. Error-code support: uses the IErrorCodeEnum enum for unified error-info management.
 * 2. Parameterized messages: supports placeholders in error messages with dynamic argument substitution.
 * 3. Exception chaining: supports wrapping underlying exceptions and preserves the full stack.
 * 4. Logging control: supports toggling automatic exception logging.
 *
 * Constructors:
 * - errorCode: creates the exception from an error code and formats the error message automatically.
 * - errorCode + args: creates the exception from an error code with arguments, supporting parameterized messages.
 * - errorCode + cause: creates the exception from an error code and a cause, preserving the exception chain.
 * - errorCode + cause + args: full-parameter variant supporting error code, cause, and arguments.
 * - errorCode + cause + log: supports toggling logging.
 *
 * Error-code mechanics:
 * - An error code carries an error code value and an error description (displayText).
 * - The description supports MessageFormat templates with {0}, {1}, ... placeholders.
 * - Arguments are passed via the args array and substituted into placeholders automatically.
 *
 * Use cases:
 * - Business validation failures.
 * - Data-operation errors.
 * - Permission-check failures.
 * - Business-rule violations.
 *
 * Notes:
 * - Inherits from CustomRuntimeException, automatically supporting message formatting and stack control.
 * - The errorCode's displayText is used as the exception message.
 * - Arguments are stored in the params property for later processing.
 *
 * @since 1.0.0
 */
class ServiceException : CustomRuntimeException {

    lateinit var errorCode: IErrorCodeEnum

    var params: Array<Any?>? = null
        private set

    constructor(message: String, vararg args: Any?) : super(message, *args)

    constructor(cause: Throwable) : super(cause)

    constructor(cause: Throwable, message: String, vararg args: Any?): super(cause, message, *args)

    constructor(cause: Throwable, message: String, log: Boolean, vararg args: Any?): super(cause, message, log, *args)

    constructor(errorCode: IErrorCodeEnum, printAllStackTrace: Boolean = false) {
        this.errorCode = errorCode
        resolveException(errorCode, printAllStackTrace)
    }

    constructor(errorCode: IErrorCodeEnum, printAllStackTrace: Boolean = false, vararg args: Any?) {
        this.errorCode = errorCode
        @Suppress("UNCHECKED_CAST")
        this.params = args.copyOf() as Array<Any?>
        resolveException(errorCode, printAllStackTrace, *args)
    }

    constructor(errorCode: IErrorCodeEnum, cause: Throwable, vararg args: Any?) {
        this.errorCode = errorCode
        resolveCauseException(cause, errorCode.displayText, *args)
    }

    constructor(errorCode: IErrorCodeEnum, cause: Throwable, log: Boolean, vararg args: Any?) {
        this.errorCode = errorCode
        resolveCauseException(cause, log, errorCode.displayText, *args)
    }

    constructor(errorCode: IErrorCodeEnum, ex: Throwable) {
        this.errorCode = errorCode
        resolveCauseException(ex, errorCode.displayText)
    }

    companion object {
        private const val serialVersionUID = 1620536616422855704L
    }

}
