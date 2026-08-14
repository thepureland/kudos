package io.kudos.ability.web.springmvc.server

import io.kudos.base.net.http.HttpClientKit
import io.kudos.context.kit.SpringKit
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.assertEquals


/**
 * Base class for web server test cases.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Import(
    HelloWorldController::class,
    KotlinRequestBodyController::class
)
@AutoConfigureRestTestClient
abstract class BaseWebServerTest {

    @Autowired
    private lateinit var restTestClient: RestTestClient

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    /** Simple name of the [ServletWebServerFactory] implementation this subclass expects to be in use. */
    protected abstract val expectedFactoryType: String

    /**
     * The container each subclass forces via `kudos.ability.web.springmvc.server` must be the one that
     * actually booted.
     *
     * Without this assertion the Tomcat/Jetty split is unverified: both containers sit on the test classpath,
     * and Spring Boot resolves that tie in Tomcat's favour, so a Jetty test that quietly ran on Tomcat would
     * still pass every request assertion below while proving nothing at all about Jetty.
     */
    @Test
    fun testConfiguredContainerIsTheOneRunning() {
        val factory = applicationContext.getBean(ServletWebServerFactory::class.java)
        assertEquals(expectedFactoryType, factory::class.java.simpleName)
    }

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
