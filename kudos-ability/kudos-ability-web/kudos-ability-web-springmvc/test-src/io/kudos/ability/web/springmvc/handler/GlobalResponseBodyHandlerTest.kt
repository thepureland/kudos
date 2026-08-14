package io.kudos.ability.web.springmvc.handler

import io.kudos.base.annotations.IgnoreApiResponseWrap
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.model.response.ApiResponse
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

/**
 * junit test for GlobalResponseBodyHandler
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class GlobalResponseBodyHandlerTest {

    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        KudosContextHolder.clear()
        mockMvc = MockMvcBuilders.standaloneSetup(
            WrappedController(),
            IgnoredController()
        )
            .setControllerAdvice(GlobalResponseBodyHandler(objectMapper))
            .build()
    }

    @Test
    fun fillTraceIdFromKudosContext() {
        val context = KudosContext()
        context.traceKey = "trace-123"
        KudosContextHolder.set(context)

        mockMvc.perform(get("/test/response/object"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.traceId").value("trace-123"))
    }

    @Test
    fun wrapNormalObjectResponse() {
        mockMvc.perform(get("/test/response/object"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message").value(""))
            .andExpect(jsonPath("$.data.name").value("kudos"))
    }

    @Test
    fun keepApiResponseUnchanged() {
        mockMvc.perform(get("/test/response/api"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message").value("already wrapped"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    /**
     * A message the controller set deliberately must survive untouched.
     *
     * This handler used to scrub any success message equal to `SUCCESS.displayText`, papering over
     * [ApiResponse.success] injecting that unresolved i18n key in the first place. With the key gone at source,
     * the scrubbing is not merely redundant — it would silently discard a caller's own message.
     */
    @Test
    fun explicitSuccessMessageIsPreserved() {
        mockMvc.perform(get("/test/response/api-explicit-message"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message").value("Saved"))
    }

    /**
     * A `String` return value is hand-serialized to JSON here, so the response must say so.
     *
     * Labelling JSON as `text/plain` leaves clients to sniff the payload, and — because
     * `StringHttpMessageConverter` derives its charset from the declared content type — also decided the
     * encoding: without a JSON content type it falls back to ISO-8859-1 and mangles every non-ASCII character.
     * The payload below is deliberately non-ASCII so that a regression shows up as corrupted text.
     */
    @Test
    fun wrapStringResponseAsJsonStringWithJsonContentType() {
        val response = mockMvc.perform(get("/test/response/string"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()
            .response

        val json = objectMapper.readTree(response.getContentAsString(Charsets.UTF_8))
        kotlin.test.assertEquals(true, json["success"].booleanValue())
        kotlin.test.assertEquals("200", json["code"].stringValue())
        kotlin.test.assertEquals("你好 hello", json["data"].stringValue())
    }

    @Test
    fun ignoreWrapAtMethodLevel() {
        mockMvc.perform(get("/test/response/ignore-method"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("raw"))
            .andExpect(jsonPath("$.success").doesNotExist())
    }

    @Test
    fun ignoreWrapAtClassLevel() {
        mockMvc.perform(get("/test/response/ignore-class"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("raw"))
            .andExpect(jsonPath("$.success").doesNotExist())
    }

    // region direct-call branch tests

    private fun callBeforeBodyWrite(handler: GlobalResponseBodyHandler, body: Any?): Any? {
        val method = WrappedController::class.java.getDeclaredMethod("objectResponse")
        return handler.beforeBodyWrite(
            body,
            org.springframework.core.MethodParameter(method, -1),
            MediaType.APPLICATION_JSON,
            org.springframework.http.converter.json.JacksonJsonHttpMessageConverter::class.java,
            org.springframework.http.server.ServletServerHttpRequest(org.springframework.mock.web.MockHttpServletRequest()),
            org.springframework.http.server.ServletServerHttpResponse(org.springframework.mock.web.MockHttpServletResponse())
        )
    }

    @Test
    fun failureResponse_getsTraceIdBackfilled() {
        val context = KudosContext()
        context.traceKey = "trace-fail"
        KudosContextHolder.set(context)
        try {
            val body = ApiResponse.fail<Any?>("400", "oops", null)
            val result = callBeforeBodyWrite(GlobalResponseBodyHandler(objectMapper), body) as ApiResponse.Failure
            kotlin.test.assertEquals("trace-fail", result.traceId)
            kotlin.test.assertEquals("oops", result.message)
        } finally {
            KudosContextHolder.clear()
        }
    }

    @Test
    fun traceIdAlreadyMatching_returnsSameInstance() {
        val context = KudosContext()
        context.traceKey = "trace-same"
        KudosContextHolder.set(context)
        try {
            val body = ApiResponse.Success<Any?>(
                code = CommonErrorCodeEnum.SUCCESS.code,
                message = "hi",
                data = null,
                traceId = "trace-same"
            )
            val result = callBeforeBodyWrite(GlobalResponseBodyHandler(objectMapper), body)
            kotlin.test.assertSame(body, result)
        } finally {
            KudosContextHolder.clear()
        }
    }

    @Test
    fun nullBody_wrapsIntoSuccessWithNullData() {
        val result = callBeforeBodyWrite(GlobalResponseBodyHandler(objectMapper), null) as ApiResponse.Success<*>
        kotlin.test.assertTrue(result.success)
        kotlin.test.assertEquals(CommonErrorCodeEnum.SUCCESS.code, result.code)
        kotlin.test.assertEquals(null, result.data)
        // The factory must not smuggle an unresolved i18n key into the message.
        kotlin.test.assertEquals("", result.message)
    }

    // endregion

    @RestController
    @RequestMapping("/test/response")
    private class WrappedController {

        @GetMapping("/object")
        fun objectResponse(): Payload {
            return Payload("kudos")
        }

        @GetMapping("/api")
        fun apiResponse(): ApiResponse<Any> {
            return ApiResponse.success("already wrapped")
        }

        @GetMapping("/api-explicit-message")
        fun apiExplicitMessage(): ApiResponse<Any> =
            ApiResponse.Success(
                code = CommonErrorCodeEnum.SUCCESS.code,
                message = "Saved",
                data = null
            )

        @GetMapping("/string")
        fun stringResponse(): String {
            return "你好 hello"
        }

        @IgnoreApiResponseWrap
        @GetMapping("/ignore-method")
        fun ignoreMethod(): Payload {
            return Payload("raw")
        }

    }

    @IgnoreApiResponseWrap
    @RestController
    @RequestMapping("/test/response")
    private class IgnoredController {

        @GetMapping("/ignore-class")
        fun ignoreClass(): Payload {
            return Payload("raw")
        }

    }

    private data class Payload(
        val name: String
    )

}
