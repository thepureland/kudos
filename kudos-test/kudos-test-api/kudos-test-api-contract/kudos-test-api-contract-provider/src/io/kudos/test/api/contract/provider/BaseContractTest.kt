package io.kudos.test.api.contract.provider

import io.restassured.module.mockmvc.RestAssuredMockMvc
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

/**
 * Spring Cloud Contract provider 端契约测试的基类。
 *
 * 责任：
 * - 拉起 SpringBoot 上下文 + MockMvc（不真正起 servlet 容器，走 Spring 内嵌路由）
 * - 在每个测试方法前把 [RestAssuredMockMvc] 的全局 mockMvc 绑定到当前 Spring 容器的 [MockMvc]，
 *   让生成的契约测试代码（[io.restassured.module.mockmvc.RestAssuredMockMvc.given]）走同一套 mvc 栈
 *
 * 子类按 `<group>.<artifact>.BaseContract<scenario>Test` 命名继承本类，
 * 由 Spring Cloud Contract Verifier 自动按 contract yml 生成具体 test 方法。
 *
 * @author K
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseContractTest {

    /** Spring MVC 测试入口，由 [AutoConfigureMockMvc] 注入 */
    @Resource
    protected lateinit var mockMvc: MockMvc

    /**
     * 把当前 [mockMvc] 绑到 RestAssured 的全局 mvc，确保生成的契约测试走同一上下文。
     * @author K
     * @since 1.0.0
     */
    @BeforeEach
    fun setup() {
        RestAssuredMockMvc.mockMvc(mockMvc)
    }

}
