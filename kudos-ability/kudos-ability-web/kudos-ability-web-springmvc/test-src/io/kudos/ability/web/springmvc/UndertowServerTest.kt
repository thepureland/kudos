package io.kudos.ability.web.springmvc

import io.kudos.test.common.TestSpringBootContextLoader
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(loader = UndertowServerTest.UndertowServerTestContextLoader::class)
open class UndertowServerTest : BaseWebServerTest() {

    class UndertowServerTestContextLoader : TestSpringBootContextLoader() {

        override fun getDynamicProperties(): Map<String, String> {
            return mapOf(
                "kudos.ability.web.springmvc.server" to "UNDERTOW"
            )
        }

    }

}