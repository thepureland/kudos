package io.kudos.ability.web.springmvc.server

import io.kudos.ability.web.springmvc.init.SwitchingServletWebServerFactory
import io.kudos.base.net.http.HttpClientKit
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

/**
 * web server测试用例基类
 *
 * @author K
 * @since 1.0.0
 */
@Import(
    HelloWorldController::class,
    SwitchingServletWebServerFactory::class
)
//@EnableAutoConfiguration(exclude = [ServletWebServerFactoryAutoConfiguration::class])
abstract class BaseWebServerTest {

//    @LocalServerPort
//    private val port = 0

    @Test
    fun testGetHelloWorld() {
        val port = SpringKit.getProperty("local.server.port")
        val url = "http://localhost:$port/test/hello"
        val response = HttpClientKit.get<String>(url)
        assertEquals("Hello World!", response.body())
    }

}
