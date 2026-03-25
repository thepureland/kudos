package io.kudos.ability.web.springmvc.handler

import io.kudos.base.enums.impl.CommonErrorCodeEnum
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
 * junit test for GlobalExceptionHandlerTest
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class GlobalExceptionHandlerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean()
        validator.afterPropertiesSet()
        mockMvc = MockMvcBuilders.standaloneSetup(TestController())
            .setControllerAdvice(GlobalExceptionHandler())
            .setValidator(validator)
            .build()
    }

    @Test
    fun handleServiceException() {
        mockMvc.perform(get("/test/global-exception/service"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("4002"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BUSINESS_ERROR.displayText))
    }

    @Test
    fun handleMethodArgumentNotValidException() {
        mockMvc.perform(
            post("/test/global-exception/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.VALIDATION_ERROR.displayText))
    }

    @Test
    fun handleConstraintViolationException() {
        mockMvc.perform(get("/test/global-exception/constraint"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.VALIDATION_ERROR.displayText))
    }

    @Test
    fun handleMissingServletRequestParameterException() {
        mockMvc.perform(get("/test/global-exception/param"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    @Test
    fun handleMethodArgumentTypeMismatchException() {
        mockMvc.perform(get("/test/global-exception/type").param("age", "abc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    @Test
    fun handleHttpMessageNotReadableException() {
        mockMvc.perform(
            post("/test/global-exception/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    @Test
    fun handleRequireException() {
        mockMvc.perform(get("/test/global-exception/require"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    @Test
    fun handleCheckException() {
        mockMvc.perform(get("/test/global-exception/check"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.BAD_REQUEST.displayText))
    }

    @Test
    fun handleException() {
        mockMvc.perform(get("/test/global-exception/runtime"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("500"))
            .andExpect(jsonPath("$.message").value(CommonErrorCodeEnum.SYSTEM_ERROR.displayText))
    }

    private class TestRequest {

        @field:NotBlank(message = "名称不能为空")
        var name: String? = null

    }

    @RestController
    @RequestMapping("/test/global-exception")
    private class TestController {

        @GetMapping("/service")
        fun service(): String {
            throw ServiceException(CommonErrorCodeEnum.BUSINESS_ERROR)
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

        @GetMapping("/system")
        fun system(): String {
            throw IllegalStateException("boom")
        }

        @GetMapping("/require")
        fun requireCase(): String {
            require(false) { "name 不能为空" }
            return "ok"
        }

        @GetMapping("/check")
        fun checkCase(): String {
            check(false) { "状态不合法" }
            return "ok"
        }

        @GetMapping("/runtime")
        fun runtime(): String {
            throw RuntimeException("boom")
        }

    }

    private class ConstraintTarget {

        @field:NotNull(message = "数量不能为空")
        var count: Int? = null

    }

}
