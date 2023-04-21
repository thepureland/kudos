package io.kudos.ability.web.springmvc

import io.kudos.test.common.TestSpringBootContextLoader
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(loader = JettyServerTest.JettyServerTestContextLoader::class)
open class JettyServerTest : BaseWebServerTest() {

    class JettyServerTestContextLoader : TestSpringBootContextLoader() {

        override fun getDynamicProperties(): Map<String, String> {
            return mapOf(
                "kudos.ability.web.springmvc.server" to "JETTY"
            )
        }

    }

}