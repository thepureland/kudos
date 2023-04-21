package io.kudos.ability.web.springmvc

import io.kudos.base.data.json.JsonKit
import io.kudos.base.net.http.HttpClientKit
import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


/**
 * web server测试用例基类
 *
 * @author K
 * @since 5.0.0
 */
@Import(MockController::class)
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class BaseWebServerTest {

    @Test
    fun get() {
        val url = "http://localhost:8880/test/get"
        val httpClientBuilder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
        val httpRequestBuilder = HttpRequest.newBuilder()
            .timeout(Duration.ofSeconds(5))
            .uri(URI.create(url))
        val result = HttpClientKit.request(httpClientBuilder, httpRequestBuilder, HttpResponse.BodyHandlers.ofString())
        assert(JsonKit.getPropertyValue(result.body(), "data") == "get")
    }

}