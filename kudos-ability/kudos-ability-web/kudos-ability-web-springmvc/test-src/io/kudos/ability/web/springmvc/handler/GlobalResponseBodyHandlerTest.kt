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

    @Test
    fun clearSuccessPlaceholderWhenControllerReturnsApiResponse() {
        mockMvc.perform(get("/test/response/api-success-placeholder"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message").value(""))
    }

    @Test
    fun wrapStringResponseAsJsonString() {
        val responseBody = mockMvc.perform(get("/test/response/string"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response
            .contentAsString

        val json = objectMapper.readTree(responseBody)
        kotlin.test.assertEquals(true, json["success"].asBoolean())
        kotlin.test.assertEquals("200", json["code"].asText())
        kotlin.test.assertEquals("hello", json["data"].asText())
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

        @GetMapping("/api-success-placeholder")
        fun apiSuccessPlaceholder(): ApiResponse<Any> =
            ApiResponse(
                success = true,
                code = CommonErrorCodeEnum.SUCCESS.code,
                message = CommonErrorCodeEnum.SUCCESS.displayText,
                data = null
            )

        @GetMapping("/string")
        fun stringResponse(): String {
            return "hello"
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
