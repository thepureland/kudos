package io.kudos.ability.web.springmvc

import io.kudos.test.common.TestSpringBootContextLoader
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(loader = TomcatServerTest.TomcatServerTestContextLoader::class)
open class TomcatServerTest : BaseWebServerTest() {

    class TomcatServerTestContextLoader : TestSpringBootContextLoader() {

        override fun getDynamicProperties(): Map<String, String> {
            return mapOf(
                "kudos.ability.web.springmvc.server" to "TOMCAT"
            )
        }

    }

}