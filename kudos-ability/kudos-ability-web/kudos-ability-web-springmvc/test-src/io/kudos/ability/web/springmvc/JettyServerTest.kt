package io.kudos.ability.web.springmvc

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource


/**
 * jetty服务器测试用例
 *
 * @author K
 * @since 5.0.0
 */
open class JettyServerTest : BaseWebServerTest() {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.web.springmvc.server") { "JETTY" }
        }

    }

}