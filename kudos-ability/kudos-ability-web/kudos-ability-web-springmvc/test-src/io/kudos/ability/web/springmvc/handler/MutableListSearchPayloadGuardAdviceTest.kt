package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import java.io.ByteArrayInputStream

/**
 * junit test for MutableListSearchPayloadGuardAdvice
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MutableListSearchPayloadGuardAdviceTest {

    private val advice = MutableListSearchPayloadGuardAdvice()
    private val converterType: Class<out HttpMessageConverter<*>> = JacksonJsonHttpMessageConverter::class.java

    @Test
    /** ListSearchPayload 参数应被纳入 Advice 处理范围 */
    fun supports_listSearchPayloadParameter_returnsTrue() {
        val parameter = methodParameter("acceptMutable", MutableListSearchPayload::class.java)
        val targetType = parameter.parameterType
        assertTrue(advice.supports(parameter, targetType, converterType))
    }

    @Test
    /** 非 ListSearchPayload 参数不应触发 Advice */
    fun supports_nonListSearchPayloadParameter_returnsFalse() {
        val parameter = methodParameter("acceptPlain", String::class.java)
        val targetType = parameter.parameterType
        assertFalse(advice.supports(parameter, targetType, converterType))
    }

    @Test
    /** 外部请求体若为 MutableListSearchPayload 必须被拒绝 */
    fun afterBodyRead_rejectsMutableListSearchPayload() {
        val parameter = methodParameter("acceptMutable", MutableListSearchPayload::class.java)
        val ex = assertThrows(ServiceException::class.java) {
            advice.afterBodyRead(
                MutableListSearchPayload(),
                emptyInputMessage(),
                parameter,
                parameter.parameterType,
                converterType
            )
        }
        assertEquals(CommonErrorCodeEnum.BAD_REQUEST.code, ex.errorCode.code)
    }

    @Test
    /** 业务查询载体（ListSearchPayload 子类）应允许通过 */
    fun afterBodyRead_allowsBusinessQueryPayload() {
        val parameter = methodParameter("acceptQuery", Query::class.java)
        val payload = Query()
        val result = advice.afterBodyRead(
            payload,
            emptyInputMessage(),
            parameter,
            parameter.parameterType,
            converterType
        )
        assertSame(payload, result)
    }

    private fun methodParameter(methodName: String, parameterType: Class<*>): MethodParameter {
        val method = TestController::class.java.getDeclaredMethod(methodName, parameterType)
        return MethodParameter(method, 0)
    }

    private fun emptyInputMessage(): HttpInputMessage = object : HttpInputMessage {
        override fun getHeaders(): HttpHeaders = HttpHeaders()
        override fun getBody() = ByteArrayInputStream(ByteArray(0))
    }

    @Suppress("unused")
    private class TestController {
        fun acceptMutable(payload: MutableListSearchPayload) = payload
        fun acceptQuery(payload: Query) = payload
        fun acceptPlain(payload: String) = payload
    }

    private class Query : ListSearchPayload()
}
