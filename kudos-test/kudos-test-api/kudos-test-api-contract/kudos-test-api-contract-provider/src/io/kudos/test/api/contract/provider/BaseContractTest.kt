package io.kudos.test.api.contract.provider

import io.restassured.module.mockmvc.RestAssuredMockMvc
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

/**
 * Base class for Spring Cloud Contract provider-side contract tests.
 *
 * Responsibilities:
 * - Boots a Spring Boot context + MockMvc (no real servlet container; uses Spring's in-memory routing).
 * - Before each test method, binds [RestAssuredMockMvc]'s global mockMvc to the current Spring
 *   container's [MockMvc], so the generated contract test code
 *   ([io.restassured.module.mockmvc.RestAssuredMockMvc.given]) goes through the same MVC stack.
 *
 * Subclasses extend this class with names like `<group>.<artifact>.BaseContract<scenario>Test`, and the
 * Spring Cloud Contract Verifier auto-generates concrete test methods from contract yml files.
 *
 * @author K
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseContractTest {

    /** Spring MVC test entry, injected by [AutoConfigureMockMvc]. */
    @Resource
    protected lateinit var mockMvc: MockMvc

    /**
     * Binds the current [mockMvc] to RestAssured's global mvc, ensuring generated contract tests run against the same context.
     * @author K
     * @since 1.0.0
     */
    @BeforeEach
    fun setup() {
        RestAssuredMockMvc.mockMvc(mockMvc)
    }

}
