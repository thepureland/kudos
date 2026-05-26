package io.kudos.ability.web.springmvc.handler

import io.kudos.base.logger.LogFactory
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter
import java.lang.reflect.Type

/**
 * Rejects using [MutableListSearchPayload] as an external request body.
 *
 * This interceptor runs after Spring MVC completes JSON -> object deserialization;
 * if the request body object is a [MutableListSearchPayload], it throws a parameter
 * exception to prevent untrusted input from directly driving a mutable query payload.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@ControllerAdvice(annotations = [Controller::class])
class MutableListSearchPayloadGuardAdvice : RequestBodyAdviceAdapter() {

    private val log = LogFactory.getLog(this::class)

    override fun supports(
        methodParameter: MethodParameter,
        targetType: Type,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean = ListSearchPayload::class.java.isAssignableFrom(methodParameter.parameterType)

    override fun afterBodyRead(
        body: Any,
        inputMessage: HttpInputMessage,
        parameter: MethodParameter,
        targetType: Type,
        converterType: Class<out HttpMessageConverter<*>>
    ): Any {
        if (body is MutableListSearchPayload) {
            log.warn("Rejected MutableListSearchPayload for request body at ${parameter.executable}")
            throw ServiceException(CommonErrorCodeEnum.BAD_REQUEST)
        }
        log.debug("Request payload accepted: ${body::class.simpleName}")
        return body
    }

}
