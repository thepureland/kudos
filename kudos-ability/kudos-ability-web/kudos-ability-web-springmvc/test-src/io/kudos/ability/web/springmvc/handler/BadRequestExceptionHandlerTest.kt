package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.model.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.TypeMismatchException
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import kotlin.test.assertEquals
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
 * junit test for BadRequestExceptionHandlerTest
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class BadRequestExceptionHandlerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean()
        validator.afterPropertiesSet()
        mockMvc = MockMvcBuilders.standaloneSetup(TestController())
            .setControllerAdvice(BadRequestExceptionHandler())
            .setValidator(validator)
            .build()
    }

    @Test
    fun handleMethodArgumentNotValid() {
        mockMvc.perform(
            post("/test/bad-request/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.message").value("name must not be blank"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
    }

    @Test
    fun handleMissingServletRequestParameter() {
        mockMvc.perform(get("/test/bad-request/param"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("Missing request parameter: name"))
    }

    @Test
    fun handleHttpMessageNotReadable() {
        mockMvc.perform(
            post("/test/bad-request/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("Malformed request body"))
    }

    @Test
    fun handleTypeMismatch() {
        mockMvc.perform(get("/test/bad-request/type").param("age", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("Invalid parameter type: age"))
    }

    // region direct-call branch tests

    @Test
    fun handleBindException_fieldAndGlobalErrors_nullDefaultMessageFallsBack() {
        val handler = BadRequestExceptionHandler()
        val bindingResult = BeanPropertyBindingResult(TestRequest(), "form")
        bindingResult.addError(FieldError("form", "name", "bad-value", false, null, null, null))
        bindingResult.addError(ObjectError("form", null, null, null))

        val response = handler.handleBindException(BindException(bindingResult))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as ApiResponse.Failure
        assertEquals(CommonErrorCodeEnum.VALIDATION_ERROR.code, body.code)
        // null defaultMessage falls back to the VALIDATION_ERROR display text
        assertEquals(CommonErrorCodeEnum.VALIDATION_ERROR.displayText, body.message)
        val errors = body.errors!!
        assertEquals(2, errors.size)
        // field errors come first, carrying the rejected value; global errors follow without one
        assertEquals("name", errors[0].field)
        assertEquals("bad-value", errors[0].rejectedValue)
        assertEquals("form", errors[1].target)
        assertEquals(CommonErrorCodeEnum.VALIDATION_ERROR.displayText, errors[1].message)
    }

    @Test
    fun handleBindException_noErrors_usesDefaultValidationMessage() {
        val handler = BadRequestExceptionHandler()
        val bindingResult = BeanPropertyBindingResult(TestRequest(), "form")

        val response = handler.handleBindException(BindException(bindingResult))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as ApiResponse.Failure
        assertEquals(CommonErrorCodeEnum.VALIDATION_ERROR.displayText, body.message)
        assertEquals(0, body.errors!!.size)
    }

    @Test
    fun handleMethodArgumentNotValid_noErrors_usesDefaultValidationMessage() {
        val handler = BadRequestExceptionHandler()
        val method = TestController::class.java.getDeclaredMethod("body", TestRequest::class.java)
        val ex = MethodArgumentNotValidException(
            MethodParameter(method, 0),
            BeanPropertyBindingResult(TestRequest(), "form")
        )

        // Dispatch through the public entry point (the override itself is protected).
        val response = handler.handleException(ex, ServletWebRequest(MockHttpServletRequest()))!!

        val body = response.body as ApiResponse.Failure
        assertEquals(CommonErrorCodeEnum.VALIDATION_ERROR.code, body.code)
        assertEquals(CommonErrorCodeEnum.VALIDATION_ERROR.displayText, body.message)
    }

    @Test
    fun handleTypeMismatch_plainTypeMismatch_usesBadRequestDisplayText() {
        val handler = BadRequestExceptionHandler()
        val ex = TypeMismatchException("abc", Int::class.java)

        val response = handler.handleException(ex, ServletWebRequest(MockHttpServletRequest()))!!

        val body = response.body as ApiResponse.Failure
        assertEquals(CommonErrorCodeEnum.BAD_REQUEST.code, body.code)
        assertEquals(CommonErrorCodeEnum.BAD_REQUEST.displayText, body.message)
    }

    // endregion

    private class TestRequest {

        @field:NotBlank(message = "name must not be blank")
        var name: String? = null

    }

    @RestController
    @RequestMapping("/test/bad-request")
    private class TestController {

        @PostMapping("/body")
        fun body(@RequestBody @Valid request: TestRequest): String {
            return request.name ?: ""
        }

        @GetMapping("/param")
        fun param(@RequestParam name: String): String {
            return name
        }

        @GetMapping("/type")
        fun type(@RequestParam age: Int): Int {
            return age
        }

    }

}
