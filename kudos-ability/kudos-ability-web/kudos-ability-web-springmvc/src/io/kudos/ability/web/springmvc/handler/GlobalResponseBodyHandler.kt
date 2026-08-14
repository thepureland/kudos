package io.kudos.ability.web.springmvc.handler

import io.kudos.base.annotations.IgnoreApiResponseWrap
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
 * @author AI: Claude
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
            return enrichTraceId(body as ApiResponse<Any>)
        }

        val wrapped = enrichTraceId(ApiResponse.success(body))

        if (!StringHttpMessageConverter::class.java.isAssignableFrom(selectedConverterType)) {
            return wrapped
        }

        // A String return value is written by StringHttpMessageConverter, which would otherwise label this
        // hand-serialized JSON as text/plain — a payload whose declared type contradicts its content, leaving
        // clients to sniff it. Declaring application/json also settles the encoding: the converter picks its
        // charset from the content type and answers UTF-8 for JSON, instead of falling back to its own default
        // of ISO-8859-1 and mangling every non-ASCII character in the response.
        response.headers.contentType = MediaType.APPLICATION_JSON
        return objectMapper.writeValueAsString(wrapped)
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

}
