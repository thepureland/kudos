package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.response.ApiResponse
import io.kudos.base.model.response.ErrorDetail
import org.springframework.beans.TypeMismatchException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Bad request exception handler.
 *
 * Intercepts bad request exceptions thrown by Spring MVC during request binding,
 * parameter conversion, request body parsing, and Bean Validation, and uniformly
 * converts them into ApiResponse.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class BadRequestExceptionHandler : ResponseEntityExceptionHandler() {

    /** Logger; only logs at WARN level — parameter errors are client-side issues and do not warrant ERROR-level alert noise. */
    private val log = LogFactory.getLog(this::class)

    /**
     * Handles parameter validation exceptions triggered by @RequestBody combined with @Valid.
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors = toErrorDetails(ex.bindingResult)
        val message = errors.firstOrNull()?.message ?: CommonErrorCodeEnum.VALIDATION_ERROR.displayText
        log.warn("MethodArgumentNotValidException: $message")
        return createResponseEntity(message, errors, headers, status, CommonErrorCodeEnum.VALIDATION_ERROR.code)
    }

    /**
     * Handles validation exceptions during form binding and query/path parameter binding.
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
    ): ResponseEntity<Any> {
        val errors = toErrorDetails(ex.bindingResult)
        val message = errors.firstOrNull()?.message ?: CommonErrorCodeEnum.VALIDATION_ERROR.displayText
        log.warn("BindException: $message")
        return createResponseEntity(message, errors, HttpHeaders(), HttpStatus.BAD_REQUEST, CommonErrorCodeEnum.VALIDATION_ERROR.code)
    }

    /**
     * Handles exceptions for missing required request parameters.
     */
    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val message = "Missing request parameter: ${ex.parameterName}"
        log.warn("MissingServletRequestParameterException: $message")
        return createResponseEntity(message, null, headers, status, CommonErrorCodeEnum.BAD_REQUEST.code)
    }

    /**
     * Handles exceptions when the request body cannot be parsed, e.g. invalid JSON.
     */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val message = "Malformed request body"
        log.warn("HttpMessageNotReadableException: ${ex.message}")
        return createResponseEntity(message, null, headers, status, CommonErrorCodeEnum.BAD_REQUEST.code)
    }

    /**
     * Handles exceptions for parameter type conversion failures.
     */
    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val message = if (ex is MethodArgumentTypeMismatchException) {
            "Invalid parameter type: ${ex.name}"
        } else {
            CommonErrorCodeEnum.BAD_REQUEST.displayText
        }
        log.warn("TypeMismatchException: $message")
        return createResponseEntity(message, null, headers, status, CommonErrorCodeEnum.BAD_REQUEST.code)
    }

    /**
     * Builds a failure response in a unified format.
     * Wraps the error code, message, and details into [ApiResponse.fail], returning it with
     * the original headers / status.
     *
     * @param message human-readable error message
     * @param errors field-level error details; null means no details
     * @param headers original response headers
     * @param status HTTP status code
     * @param code business error code (derived from [CommonErrorCodeEnum])
     * @return ResponseEntity in unified format
     * @author K
     * @since 1.0.0
     */
    private fun createResponseEntity(
        message: String,
        errors: List<ErrorDetail>?,
        headers: HttpHeaders,
        status: HttpStatusCode,
        code: String
    ): ResponseEntity<Any> {
        val body = ApiResponse.fail<Any?>(code, message, errors = errors)
        return ResponseEntity(body, headers, status)
    }

    /**
     * Converts the Spring validation result into a structured list of error details.
     * Field-level errors include the rejectedValue from [BindingResult], helping the
     * frontend locate the specific user-entered value; global errors (object-level
     * assertions) do not include a rejectedValue. The two categories are ordered
     * "field errors first", consistent with common form displays.
     *
     * @param bindingResult Spring validation result
     * @return combined list of field errors plus global errors
     * @author K
     * @since 1.0.0
     */
    private fun toErrorDetails(bindingResult: BindingResult): List<ErrorDetail> {
        val fieldErrors = bindingResult.fieldErrors.map {
            ErrorDetail(
                code = it.code,
                field = it.field,
                target = it.objectName,
                message = it.defaultMessage ?: CommonErrorCodeEnum.VALIDATION_ERROR.displayText,
                rejectedValue = it.rejectedValue
            )
        }
        val globalErrors = bindingResult.globalErrors.map {
            ErrorDetail(
                code = it.code,
                target = it.objectName,
                message = it.defaultMessage ?: CommonErrorCodeEnum.VALIDATION_ERROR.displayText
            )
        }
        return fieldErrors + globalErrors
    }

}
