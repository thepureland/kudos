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
    /** Spring 注入的 Jackson [ObjectMapper]，仅用于 [StringHttpMessageConverter] 分支手工序列化 */
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
     *
     * @param T 响应载荷类型
     * @param response 待处理的响应
     * @return 已注入 traceId 且清理过占位 message 的响应
     * @author K
     * @since 1.0.0
     */
    private fun <T> enrichAndSanitize(response: ApiResponse<T>): ApiResponse<T> {
        val withTrace = enrichTraceId(response)
        return clearUnresolvedSuccessPlaceholderMessage(withTrace)
    }

    /**
     * 将当前请求上下文中的 traceKey 回填到响应 traceId。
     * traceId 已与上下文一致时直接返回原对象，避免 [data class.copy] 带来的多余分配。
     *
     * @param T 响应载荷类型
     * @param response 待处理的响应
     * @return 若需更新则返回 copy，否则返回原对象
     * @author K
     * @since 1.0.0
     */
    private fun <T> enrichTraceId(response: ApiResponse<T>): ApiResponse<T> {
        val traceId = KudosContextHolder.get().traceKey
        if (traceId.isNullOrBlank() || response.traceId == traceId) return response
        return when (response) {
            is ApiResponse.Success -> response.copy(traceId = traceId)
            is ApiResponse.Failure -> response.copy(traceId = traceId)
        }
    }

    /**
     * 清理"成功响应"中未被 i18n 解析的占位 message（避免把 `sys.error-msg.default.200` 这种 key 暴露到前端）。
     * 只对 success 响应生效；失败响应保留原 message（其常常包含真实错因）。
     *
     * @param T 响应载荷类型
     * @param response 待处理的响应
     * @return 占位被替换为空串的响应；非命中场景原样返回
     * @author K
     * @since 1.0.0
     */
    private fun <T> clearUnresolvedSuccessPlaceholderMessage(response: ApiResponse<T>): ApiResponse<T> {
        // 占位 message 清理只针对成功响应；is Success 同时把 success 检查和子类型 narrow 一起做了
        if (response !is ApiResponse.Success || response.code != CommonErrorCodeEnum.SUCCESS.code) {
            return response
        }
        val placeholder = CommonErrorCodeEnum.SUCCESS.displayText
        if (response.message == placeholder) {
            return response.copy(message = "")
        }
        return response
    }

}
