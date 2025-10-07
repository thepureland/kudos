package io.kudos.base.net.http

import io.kudos.base.image.ImageKit
import io.kudos.base.lang.SystemKit
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * HttpClientKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class HttpClientKitTest {

    @Test
    fun request() {
        val url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png"
        val httpClientBuilder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
        val httpRequestBuilder = HttpRequest.newBuilder()
            .timeout(Duration.ofSeconds(5))
            .uri(URI.create(url))
        val response =
            HttpClientKit.asyncRequest(httpClientBuilder, httpRequestBuilder, HttpResponse.BodyHandlers.ofInputStream())
        val body = response.body()
        val image = ImageIO.read(body)
        if (SystemKit.hasGUI()) {
            ImageKit.showImage(image)
            Thread.sleep(3000)
        }
    }

}