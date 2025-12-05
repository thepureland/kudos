package io.kudos.test.api.contract.consumer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties

@SpringBootTest
@AutoConfigureStubRunner(
    ids = ["com.example:user-service:+:stubs:8081"],  // groupId:artifactId:version:classifier:port
    stubsMode = StubRunnerProperties.StubsMode.REMOTE // 如果你 stub jar 存在本地则可用 LOCAL
)
class UserClientTest {

//    @Autowired
//    lateinit var userClient: UserClient // 你封装的 RestTemplate 或 Feign client
//
//    @Test
//    fun `should get user from stub`() {
//        val user = userClient.getUserById(1L)
//        assertThat(user.name).isEqualTo("Tom")
//    }
}
