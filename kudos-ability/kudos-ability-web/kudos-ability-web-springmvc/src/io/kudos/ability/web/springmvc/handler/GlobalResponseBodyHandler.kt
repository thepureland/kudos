package io.kudos.ability.web.springmvc.handler

import io.kudos.base.annotations.IgnoreApiResponseWrap
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.model.response.ApiResponse
import io.kudos.context.core.KudosContextHolder
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import tools.jackson.databind.ObjectMapper

/**
 * 全局响应体处理器。
 *
 * 负责将控制器返回结果统一包装为 ApiResponse，
 * 并支持通过 IgnoreApiResponseWrap 注解跳过包装。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@ControllerAdvice
class GlobalResponseBodyHandler(
    private val objectMapper: ObjectMapper
) : ResponseBodyAdvice<Any> {

    /**
     * 判断当前返回结果是否需要统一包装
     */
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean =
        !returnType.containingClass.isAnnotationPresent(IgnoreApiResponseWrap::class.java) &&
            !returnType.hasMethodAnnotation(IgnoreApiResponseWrap::class.java)

    /**
     * 对返回结果进行统一包装，并兼容String类型响应
     */
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        if (body is ApiResponse<*>) {
            @Suppress("UNCHECKED_CAST")
            return enrichAndSanitize(body as ApiResponse<Any>)
        }

        val wrapped = enrichAndSanitize(ApiResponse.success(body))

        return if (StringHttpMessageConverter::class.java.isAssignableFrom(selectedConverterType)) {
            objectMapper.writeValueAsString(wrapped)
        } else {
            wrapped
        }
    }

    /**
     * 回填 traceId；若成功响应的 message 仍为未解析的 [CommonErrorCodeEnum.SUCCESS] 的 [io.kudos.base.enums.ienums.IErrorCodeEnum.displayText]
     *（即 `sys.error-msg.default.200`），则将 message 置为空串，避免对外暴露占位 key。
     */
    private fun <T> enrichAndSanitize(response: ApiResponse<T>): ApiResponse<T> {
        val withTrace = enrichTraceId(response)
        return clearUnresolvedSuccessPlaceholderMessage(withTrace)
    }

    /**
     * 将当前请求上下文中的 traceKey 回填到响应 traceId
     */
    private fun <T> enrichTraceId(response: ApiResponse<T>): ApiResponse<T> {
        val traceId = KudosContextHolder.get().traceKey
        return if (traceId.isNullOrBlank() || response.traceId == traceId) response else response.copy(traceId = traceId)
    }

    private fun <T> clearUnresolvedSuccessPlaceholderMessage(response: ApiResponse<T>): ApiResponse<T> {
        if (!response.success || response.code != CommonErrorCodeEnum.SUCCESS.code) {
            return response
        }
        val placeholder = CommonErrorCodeEnum.SUCCESS.displayText
        if (response.message == placeholder) {
            return response.copy(message = null)
        }
        return response
    }

}
