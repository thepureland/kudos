package io.kudos.ability.web.springmvc.server

import io.kudos.base.net.http.HttpClientKit
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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
@EnableAutoConfiguration(exclude = [ServletWebServerFactoryAutoConfiguration::class])
abstract class BaseWebServerTest {

//    @LocalServerPort
//    private val port = 0

    @Test
    fun testGetHelloWorld() {
        val port = SpringKit.getProperty("local.server.port")
        val url = "http://localhost:$port/test/hello"
        val httpClientBuilder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
        val httpRequestBuilder = HttpRequest.newBuilder()
            .timeout(Duration.ofSeconds(5))
            .uri(URI.create(url))
        val response =
            HttpClientKit.request(httpClientBuilder, httpRequestBuilder, HttpResponse.BodyHandlers.ofString())
        assertEquals("Hello World!", response.body())
    }

}
