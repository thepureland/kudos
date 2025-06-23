package io.kudos.ability.web.ktor.base.spring

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.kudos.ability.web.ktor.core.IKtorRouteRegistrar
import io.kudos.ability.web.ktor.core.KtorContext
import io.kudos.base.net.http.HttpClientKit
import io.kudos.test.common.init.EnableKudosTest
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * 路由注册测试
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(XxxRouteRegistrar::class)
class RouteRegistrarTest {

    @Test
    fun testByJdkHttpClient()  {
        val url = "http://localhost:8080/hi"
        val httpClientBuilder = java.net.http.HttpClient.newBuilder()
        val requestBuilder = HttpRequest.newBuilder().uri(URI.create(url))
        val responseBuilder = HttpResponse.BodyHandlers.ofString()
        val response = HttpClientKit.request(httpClientBuilder, requestBuilder, responseBuilder)
        assertEquals("Hi", response.body())
    }

    @Test
    fun testByKtorClient() = runBlocking {
        val client = HttpClient()
        val response = client.get("http://localhost:${KtorContext.properties.engine.port}/hi")
        assertEquals("Hi", response.bodyAsText())
    }

    companion object {

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.web.ktor.engine.name") { "netty" }
        }

    }

}

@Component
class XxxRouteRegistrar : IKtorRouteRegistrar {

    override fun register(routing: Routing) {
        routing.route("/hi") {
            get {
                call.respondText("Hi")
            }
        }
    }

}