package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.error.ServiceException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * junit test for GlobalExceptionHandler.
 *
 * Both advices are registered, exactly as production wires them, so the assertions below describe the answer a
 * caller actually receives rather than the answer one advice would give in isolation. That matters here: the
 * four request-shape failures at the bottom used to have a duplicate handler in [GlobalExceptionHandler] which
 * replied HTTP 200 with no error detail, and only the registration order decided which reply won.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
class GlobalExceptionHandlerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean()
        validator.afterPropertiesSet()
        mockMvc = MockMvcBuilders.standaloneSetup(TestController())
            .setControllerAdvice(BadRequestExceptionHandler(), GlobalExceptionHandler())
            .setValidator(validator)
            .build()
    }

    /** A declined business rule is normal traffic: HTTP 200, outcome in the body. */
    @Test
    fun handleServiceException() {
        mockMvc.perform(get("/test/global-exception/service"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("4002"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BUSINESS_ERROR.displayText))
    }

    /**
     * A missing record is a client addressing mistake, not a server fault: 404, and never the ERROR-level
     * SYSTEM_ERROR it used to produce by falling through to the catch-all handler.
     */
    @Test
    fun handleObjectNotFoundException() {
        mockMvc.perform(get("/test/global-exception/not-found"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("404"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.NOT_FOUND.displayText))
    }

    @Test
    fun handleConstraintViolationException() {
        mockMvc.perform(get("/test/global-exception/constraint"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.VALIDATION_ERROR.displayText))
    }

    @Test
    fun handleRequireException() {
        mockMvc.perform(get("/test/global-exception/require"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    @Test
    fun handleCheckException() {
        mockMvc.perform(get("/test/global-exception/check"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    /**
     * The one that alerting depends on: an unexpected failure must reach the wire as HTTP 500. A load balancer,
     * gateway or APM agent classifies on the status line and never opens the body, so the previous HTTP 200
     * meant genuine server errors were invisible to every alerting path the platform has.
     */
    @Test
    fun handleException_unexpectedFailureIsAnHttp500() {
        mockMvc.perform(get("/test/global-exception/runtime"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("500"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.SYSTEM_ERROR.displayText))
    }

    @Test
    fun handleIllegalArgumentWithNullMessage_fallsBackToBadRequestText() {
        // Direct call: covers the `ex.message ?: BAD_REQUEST.displayText` null branch.
        val response = GlobalExceptionHandler().handleIllegalArgumentOrStateException(IllegalArgumentException())
        kotlin.test.assertFalse(response.success)
        kotlin.test.assertEquals(CommonErrorCodeEnum.BAD_REQUEST.code, response.code)
        kotlin.test.assertEquals(CommonErrorCodeEnum.BAD_REQUEST.displayText, response.message)
    }

    // region request-shape failures are owned by BadRequestExceptionHandler

    /**
     * These four used to be handled twice, and the duplicate in [GlobalExceptionHandler] answered differently:
     * HTTP 200 and no `errors[]`. With both advices registered the answers below are what a caller gets —
     * a real 400 and a locating message — and the duplicate can no longer drift back in unnoticed.
     */
    @Test
    fun methodArgumentNotValid_isHandledAsBadRequestWithFieldDetail() {
        mockMvc.perform(
            post("/test/global-exception/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
    }

    @Test
    fun missingServletRequestParameter_isHandledAsBadRequest() {
        mockMvc.perform(get("/test/global-exception/param"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("Missing request parameter: name"))
    }

    @Test
    fun methodArgumentTypeMismatch_isHandledAsBadRequest() {
        mockMvc.perform(get("/test/global-exception/type").param("age", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("Invalid parameter type: age"))
    }

    @Test
    fun httpMessageNotReadable_isHandledAsBadRequest() {
        mockMvc.perform(
            post("/test/global-exception/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("Malformed request body"))
    }

    // endregion

    private class TestRequest {

        @field:NotBlank(message = "name must not be blank")
        var name: String? = null

    }

    @RestController
    @RequestMapping("/test/global-exception")
    private class TestController {

        @GetMapping("/service")
        fun service(): String {
            throw ServiceException(CommonErrorCodeEnum.BUSINESS_ERROR)
        }

        @GetMapping("/not-found")
        fun notFound(): String {
            throw ObjectNotFoundException("Record not found!")
        }

        @PostMapping("/body")
        fun body(@RequestBody @Valid request: TestRequest): String {
            return request.name ?: ""
        }

        @GetMapping("/constraint")
        fun constraint(): String {
            val violations = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
                .validate(ConstraintTarget())
            throw ConstraintViolationException(violations)
        }

        @GetMapping("/param")
        fun param(@RequestParam name: String): String {
            return name
        }

        @GetMapping("/type")
        fun type(@RequestParam age: Int): Int {
            return age
        }

        @GetMapping("/require")
        fun requireCase(): String {
            require(false) { "name must not be blank" }
            return "ok"
        }

        @GetMapping("/check")
        fun checkCase(): String {
            check(false) { "invalid state" }
            return "ok"
        }

        @GetMapping("/runtime")
        fun runtime(): String {
            throw RuntimeException("boom")
        }

    }

    private class ConstraintTarget {

        @field:NotNull(message = "count must not be null")
        var count: Int? = null

    }

}
