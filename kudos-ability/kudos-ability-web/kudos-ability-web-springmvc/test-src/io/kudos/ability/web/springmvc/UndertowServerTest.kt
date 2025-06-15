package io.kudos.ability.web.springmvc

import io.kudos.test.common.init.TestSpringBootContextLoader
import org.springframework.test.context.ContextConfiguration


/**
 * undertow服务器测试用例
 *
 * @author K
 * @since 1.0.0
 */
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