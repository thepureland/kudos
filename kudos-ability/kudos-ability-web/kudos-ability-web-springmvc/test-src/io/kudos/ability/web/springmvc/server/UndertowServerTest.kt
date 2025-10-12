package io.kudos.ability.web.springmvc.server

import io.kudos.test.common.init.EnableKudosTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource


/**
 * undertow容器测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(classes = [UndertowServerTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UndertowServerTest : BaseWebServerTest() {

    companion object Companion {

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.web.springmvc.server") { "UNDERTOW" }
        }

    }

}