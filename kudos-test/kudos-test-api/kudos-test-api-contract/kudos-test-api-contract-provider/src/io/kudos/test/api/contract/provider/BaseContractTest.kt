package io.kudos.test.api.contract.provider

import io.kudos.test.common.init.EnableKudosTest
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

@EnableKudosTest
@AutoConfigureMockMvc
abstract class BaseContractTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        RestAssuredMockMvc.mockMvc(mockMvc)
    }

}
