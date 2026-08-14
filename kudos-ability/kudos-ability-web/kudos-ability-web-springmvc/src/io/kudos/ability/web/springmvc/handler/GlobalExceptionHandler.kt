package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.response.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Global exception handler.
 *
 * Handles business exceptions and uncaught exceptions, converting them uniformly into `ApiResponse`.
 *
 * ## HTTP status policy
 *
 * Every handler below declares its status explicitly, and the rule is deliberate:
 *
 * | Outcome | HTTP | Rationale |
 * |---|---|---|
 * | [ServiceException] | 200 | A business rule declined the request. The call itself succeeded, so this is normal traffic — surfacing it as 4xx/5xx would drown gateway and APM alerting in ordinary business flow. The outcome is carried by `code` in the body. |
 * | [ObjectNotFoundException] | 404 | The addressed resource does not exist. |
 * | [ConstraintViolationException], [IllegalArgumentException], [IllegalStateException] | 400 | The caller sent something invalid. Matches the status [BadRequestExceptionHandler] already returns for binding and Bean Validation failures. |
 * | anything else | 500 | An unexpected server-side failure. |
 *
 * Two things this replaces are worth naming, so they are not reintroduced:
 *
 * 1. **Unexpected failures used to return HTTP 200** with a `"500"` code buried in the body. Nothing upstream —
 *    load balancer, gateway, APM — inspects a response body, so genuine server errors were invisible to every
 *    alerting path the platform has.
 * 2. **Five handlers here duplicated [BadRequestExceptionHandler]** (`MethodArgumentNotValidException`,
 *    `BindException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`,
 *    `HttpMessageNotReadableException`). Because that advice carries `@Order(HIGHEST_PRECEDENCE)` it always won,
 *    making the copies here unreachable in production while still answering differently — HTTP 200, no
 *    `errors[]` detail — anywhere they were registered alone, such as a narrower test context. One exception
 *    type now has exactly one handler and exactly one answer.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LogFactory.getLog(this::class)

    /**
     * Handles business exceptions.
     *
     * Logged at WARN, not ERROR: a declined business rule is an expected outcome, and paging an on-call
     * engineer for it is how error logs become noise nobody reads.
     */
    @ExceptionHandler(ServiceException::class)
    @ResponseStatus(HttpStatus.OK)
    fun handleServiceException(ex: ServiceException): ApiResponse<Any> {
        log.warn("ServiceException: code=${ex.errorCode.code}, message=${ex.message}")
        return ApiResponse.fail(ex.errorCode.code, ex.errorCode.displayText, null)
    }

    /**
     * Handles "the requested record does not exist".
     *
     * Previously this fell through to [handleException], which logged it at ERROR and answered
     * `SYSTEM_ERROR`. Since the CRUD base controllers raise it for any lookup that misses
     * ([io.kudos.ability.web.springmvc.controller.BaseReadOnlyController.getDetail] and
     * [io.kudos.ability.web.springmvc.controller.BaseCrudController.getEdit]), requesting a stale id — an
     * entirely routine client mistake — produced a system-error alert.
     */
    @ExceptionHandler(ObjectNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleObjectNotFoundException(ex: ObjectNotFoundException): ApiResponse<Any> {
        log.warn("ObjectNotFoundException: ${ex.message}")
        return ApiResponse.fail(CommonErrorCodeEnum.NOT_FOUND.code, CommonErrorCodeEnum.NOT_FOUND.displayText)
    }

    /**
     * Handles method-level parameter validation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ApiResponse<Any> {
        log.warn("ConstraintViolationException: ${ex.message}")
        return ApiResponse.fail(
            CommonErrorCodeEnum.VALIDATION_ERROR.code,
            CommonErrorCodeEnum.VALIDATION_ERROR.displayText
        )
    }

    /**
     * Handles parameter/state assertion exceptions triggered by Kotlin require/check.
     *
     * The assertion message is logged but deliberately kept out of the response: `require`/`check` messages are
     * written for developers and routinely name internal state, which does not belong in a client-facing payload.
     */
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentOrStateException(ex: RuntimeException): ApiResponse<Any> {
        val message = ex.message ?: CommonErrorCodeEnum.BAD_REQUEST.displayText
        log.warn("${ex::class.simpleName}: $message")
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, CommonErrorCodeEnum.BAD_REQUEST.displayText)
    }

    /**
     * Handles other uncaught exceptions.
     */
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(ex: Exception): ApiResponse<Any> {
        log.error(ex, "Unhandled exception")
        return ApiResponse.fail(CommonErrorCodeEnum.SYSTEM_ERROR.code, CommonErrorCodeEnum.SYSTEM_ERROR.displayText)
    }

}
