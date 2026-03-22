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
 * 非法参数请求异常处理。
 *
 * 负责拦截 Spring MVC 在请求绑定、参数转换、请求体解析、Bean Validation 校验阶段抛出的 bad request 异常，
 * 并统一转换为 ApiResponse。
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class BadRequestExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LogFactory.getLog(this)

    /**
     * 处理@RequestBody配合@Valid触发的参数校验异常
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors = toErrorDetails(ex.bindingResult)
        val message = errors.firstOrNull()?.message ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans
        log.warn("MethodArgumentNotValidException: $message")
        return createResponseEntity(message, errors, headers, status, CommonErrorCodeEnum.VALIDATION_ERROR.code)
    }

    /**
     * 处理表单绑定、query/path参数绑定阶段的校验异常
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
    ): ResponseEntity<Any> {
        val errors = toErrorDetails(ex.bindingResult)
        val message = errors.firstOrNull()?.message ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans
        log.warn("BindException: $message")
        return createResponseEntity(message, errors, HttpHeaders(), HttpStatus.BAD_REQUEST, CommonErrorCodeEnum.VALIDATION_ERROR.code)
    }

    /**
     * 处理缺少必填请求参数的异常
     */
    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val message = "缺少请求参数：${ex.parameterName}"
        log.warn("MissingServletRequestParameterException: $message")
        return createResponseEntity(message, null, headers, status, CommonErrorCodeEnum.BAD_REQUEST.code)
    }

    /**
     * 处理请求体无法解析的异常，例如非法JSON
     */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val message = "请求体格式错误"
        log.warn("HttpMessageNotReadableException: ${ex.message}")
        return createResponseEntity(message, null, headers, status, CommonErrorCodeEnum.BAD_REQUEST.code)
    }

    /**
     * 处理参数类型转换失败的异常
     */
    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val message = if (ex is MethodArgumentTypeMismatchException) {
            "参数类型错误：${ex.name}"
        } else {
            CommonErrorCodeEnum.BAD_REQUEST.trans
        }
        log.warn("TypeMismatchException: $message")
        return createResponseEntity(message, null, headers, status, CommonErrorCodeEnum.BAD_REQUEST.code)
    }

    /**
     * 构造统一格式的失败响应
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
     * 将Spring校验结果转换为结构化错误明细
     */
    private fun toErrorDetails(bindingResult: BindingResult): List<ErrorDetail> {
        val fieldErrors = bindingResult.fieldErrors.map {
            ErrorDetail(
                code = it.code,
                field = it.field,
                target = it.objectName,
                message = it.defaultMessage ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans,
                rejectedValue = it.rejectedValue
            )
        }
        val globalErrors = bindingResult.globalErrors.map {
            ErrorDetail(
                code = it.code,
                target = it.objectName,
                message = it.defaultMessage ?: CommonErrorCodeEnum.VALIDATION_ERROR.trans
            )
        }
        return fieldErrors + globalErrors
    }

}
