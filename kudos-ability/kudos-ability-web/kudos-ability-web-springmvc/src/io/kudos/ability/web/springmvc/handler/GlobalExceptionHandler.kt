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
 * 全局异常处理器。
 *
 * 负责处理业务异常、通用参数异常以及未捕获异常，
 * 并统一转换为 ApiResponse 返回给调用方。
 *
 * @author K
 * @since 1.0.0
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LogFactory.getLog(this::class)

    /**
     * 处理业务异常
     */
    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ApiResponse<Any> {
        log.warn("ServiceException: code=${ex.errorCode.code}, message=${ex.message}")
        return ApiResponse.fail(ex.errorCode.code, ex.message!!, null)
    }

    /**
     * 处理@RequestBody配合@Valid触发的参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ApiResponse<Any> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans
        return ApiResponse.fail(CommonErrorCodeEnum.VALIDATION_ERROR.code, message)
    }

    /**
     * 处理参数绑定阶段的校验异常
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ApiResponse<Any> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans
        return ApiResponse.fail(CommonErrorCodeEnum.VALIDATION_ERROR.code, message)
    }

    /**
     * 处理方法级参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ApiResponse<Any> {
        val message = ex.constraintViolations.firstOrNull()?.message
            ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans
        return ApiResponse.fail(CommonErrorCodeEnum.VALIDATION_ERROR.code, message)
    }

    /**
     * 处理缺少必填请求参数的异常
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, "缺少请求参数：${ex.parameterName}")
    }

    /**
     * 处理参数类型转换失败的异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, "参数类型错误：${ex.name}")
    }

    /**
     * 处理请求体无法解析的异常，例如非法JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ApiResponse<Any> {
        return ApiResponse.fail(CommonErrorCodeEnum.BAD_REQUEST.code, "请求体格式错误")
    }

    /**
     * 处理未捕获的其他异常
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ApiResponse<Any> {
        log.error("Unhandled exception", ex)
        return ApiResponse.fail(CommonErrorCodeEnum.SYSTEM_ERROR.code, CommonErrorCodeEnum.SYSTEM_ERROR.trans)
    }

}
