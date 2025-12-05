package io.kudos.ability.web.springmvc.server

import io.kudos.ability.web.springmvc.init.SwitchingServletWebServerFactory
import io.kudos.base.net.http.HttpClientKit
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.client.RestTestClient
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
@AutoConfigureRestTestClient
abstract class BaseWebServerTest {

//    @LocalServerPort
//    private val port = 0

    @Autowired
    private lateinit var restTestClient: RestTestClient

    @Test
    fun testGetHelloWorld() {
        restTestClient.get()
            .uri("/test/hello")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .value { assertEquals("Hello World!", it) }
    }

    @Test
    fun testGetHelloWorldByHttpClientKit() {
        val port = SpringKit.getProperty("local.server.port")
        val url = "http://localhost:$port/test/hello"
        val response = HttpClientKit.get<String>(url)
        assertEquals("Hello World!", response.body())
    }

}
