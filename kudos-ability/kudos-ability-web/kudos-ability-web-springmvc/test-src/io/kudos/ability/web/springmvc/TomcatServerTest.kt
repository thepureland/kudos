package io.kudos.ability.web.springmvc

import io.kudos.test.common.init.TestSpringBootContextLoader
import org.springframework.test.context.ContextConfiguration


/**
 * tomcat服务器测试用例
 *
 * @author K
 * @since 1.0.0
 */
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