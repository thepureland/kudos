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
 * Global response body handler.
 *
 * Responsible for uniformly wrapping controller return values into ApiResponse,
 * and supports skipping the wrapping via the IgnoreApiResponseWrap annotation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@ControllerAdvice
class GlobalResponseBodyHandler(
    /** Jackson [ObjectMapper] injected by Spring, used only for manual serialization in the [StringHttpMessageConverter] branch */
    private val objectMapper: ObjectMapper
) : ResponseBodyAdvice<Any> {

    /**
     * Determines whether the current return value needs to be uniformly wrapped.
     */
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean =
        !returnType.containingClass.isAnnotationPresent(IgnoreApiResponseWrap::class.java) &&
            !returnType.hasMethodAnnotation(IgnoreApiResponseWrap::class.java)

    /**
     * Uniformly wraps the return value and is compatible with String-type responses.
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
     * Backfills the traceId; if the message of a successful response is still the unresolved
     * [io.kudos.base.enums.ienums.IErrorCodeEnum.displayText] of [CommonErrorCodeEnum.SUCCESS]
     * (i.e. `sys.error-msg.default.200`), clears the message to an empty string to avoid
     * exposing the placeholder key externally.
     *
     * @param T response payload type
     * @param response the response to process
     * @return the response with traceId injected and placeholder message cleared
     * @author K
     * @since 1.0.0
     */
    private fun <T> enrichAndSanitize(response: ApiResponse<T>): ApiResponse<T> {
        val withTrace = enrichTraceId(response)
        return clearUnresolvedSuccessPlaceholderMessage(withTrace)
    }

    /**
     * Backfills the traceKey from the current request context into the response traceId.
     * Returns the original object directly when traceId already matches the context,
     * to avoid extra allocations from [data class.copy].
     *
     * @param T response payload type
     * @param response the response to process
     * @return a copy if updating is needed, otherwise the original object
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
     * Clears the placeholder message that was not resolved by i18n in a "success response"
     * (to avoid exposing keys like `sys.error-msg.default.200` to the frontend).
     * Only applies to success responses; failure responses keep their original message
     * (which often contains the real cause).
     *
     * @param T response payload type
     * @param response the response to process
     * @return the response with placeholder replaced by empty string; returns as-is when not matched
     * @author K
     * @since 1.0.0
     */
    private fun <T> clearUnresolvedSuccessPlaceholderMessage(response: ApiResponse<T>): ApiResponse<T> {
        // Placeholder message cleanup only applies to success responses; `is Success` narrows the type and checks success at once
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
