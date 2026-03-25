package io.kudos.ability.web.springmvc.handler

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
            .andExpect(jsonPath("$.message").value("名称不能为空"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
    }

    @Test
    fun handleMissingServletRequestParameter() {
        mockMvc.perform(get("/test/bad-request/param"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("缺少请求参数：name"))
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
            .andExpect(jsonPath("$.message").value("请求体格式错误"))
    }

    @Test
    fun handleTypeMismatch() {
        mockMvc.perform(get("/test/bad-request/type").param("age", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("参数类型错误：age"))
    }

    private class TestRequest {

        @field:NotBlank(message = "名称不能为空")
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
