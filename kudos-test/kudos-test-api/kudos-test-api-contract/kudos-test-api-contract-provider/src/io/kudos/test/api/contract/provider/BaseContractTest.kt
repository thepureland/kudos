package io.kudos.test.api.contract.provider

import io.restassured.module.mockmvc.RestAssuredMockMvc
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseContractTest {

    @Resource
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        RestAssuredMockMvc.mockMvc(mockMvc)
    }

}
