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
 * Base class for web server test cases.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Import(
    HelloWorldController::class,
    KotlinRequestBodyController::class,
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

    /**
     * A Kotlin data class as `@RequestBody`, over real HTTP.
     *
     * Guards a defect no unit test in this repository could see: unit tests call services directly
     * and never serialize, so the fact that Jackson could not construct *any* Kotlin data class went
     * unnoticed until the first real JSON POST — by which point roughly two dozen admin endpoints
     * had never once worked. The assertion deliberately covers a defaulted non-nullable parameter
     * (the case that needs the Kotlin module) and an omitted one (the case that needs its default).
     */
    @Test
    fun testKotlinDataClassRequestBodyDeserializes() {
        restTestClient.post()
            .uri("/test/kotlinBody")
            .header("Content-Type", "application/json")
            .body("""{"name":"widget","effect":"DENY","at":"2026-01-02T03:04:05"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .value { assertEquals("widget|DENY|-|2026-01-02T03:04:05", it) }
    }

    /** The defaulted parameter must actually take its default rather than fail or arrive null. */
    @Test
    fun testKotlinDefaultParameterIsHonoured() {
        restTestClient.post()
            .uri("/test/kotlinBody")
            .header("Content-Type", "application/json")
            .body("""{"name":"widget"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .value { assertEquals("widget|ALLOW|-|-", it) }
    }

    @Test
    fun testGetHelloWorldByHttpClientKit() {
        val port = SpringKit.getProperty("local.server.port")
        val url = "http://localhost:$port/test/hello"
        val response = HttpClientKit.get<String>(url)
        assertEquals("Hello World!", response.body())
    }

}
