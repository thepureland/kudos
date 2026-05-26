package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.response.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Global exception handler.
 *
 * Handles business exceptions, common parameter exceptions and uncaught exceptions,
 * and uniformly converts them into ApiResponse returned to the caller.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LogFactory.getLog(this::class)

    /**
     * Handles business exceptions.
     */
    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ApiResponse<Any> {
        log.warn("ServiceException: code=${ex.errorCode.code}, message=${ex.message}")
        return ApiResponse.fail(ex.errorCode.code, ex.errorCode.displayText, null)
    }

    /**
     * Handles parameter validation exceptions triggered by @RequestBody combined with @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.VALIDATION_ERROR.code, CommonErrorCodeEnum.VALIDATION_ERROR.displayText)
    }

    /**
     * Handles validation exceptions during the parameter binding stage.
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.VALIDATION_ERROR.code, CommonErrorCodeEnum.VALIDATION_ERROR.displayText)
    }

    /**
     * Handles method-level parameter validation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.VALIDATION_ERROR.code, CommonErrorCodeEnum.VALIDATION_ERROR.displayText)
    }

    /**
     * Handles exceptions for missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, CommonErrorCodeEnum.BAD_REQUEST.displayText)
    }

    /**
     * Handles exceptions for parameter type conversion failures.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, CommonErrorCodeEnum.BAD_REQUEST.displayText)
    }

    /**
     * Handles exceptions when the request body cannot be parsed, e.g. invalid JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, CommonErrorCodeEnum.BAD_REQUEST.displayText)
    }

    /**
     * Handles parameter/state assertion exceptions triggered by Kotlin require/check.
     */
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleIllegalArgumentOrStateException(ex: RuntimeException): ApiResponse<Any> {
        val message = ex.message ?: CommonErrorCodeEnum.BAD_REQUEST.displayText
        log.warn("${ex::class.simpleName}: $message")
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, CommonErrorCodeEnum.BAD_REQUEST.displayText)
    }

    /**
     * Handles other uncaught exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ApiResponse<Any> {
        log.error(ex, "Unhandled exception")
        return ApiResponse.fail(CommonErrorCodeEnum.SYSTEM_ERROR.code, CommonErrorCodeEnum.SYSTEM_ERROR.displayText)
    }

}
