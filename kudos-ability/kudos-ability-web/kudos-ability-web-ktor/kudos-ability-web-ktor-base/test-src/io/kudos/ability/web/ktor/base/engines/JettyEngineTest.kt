package io.kudos.ability.web.ktor.base.engines

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.jakarta.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * jetty引擎ktor测试
 *
 * @author K
 * @since 1.0.0
 */
class JettyEngineTest {

    private val PORT = 9592
    private var engine: EmbeddedServer<JettyApplicationEngine, *>? = null

    @BeforeTest
    fun setup() {
        engine = embeddedServer(Jetty, PORT) {
            routing {
                get("/") {
                    call.respondText("Hello World!")
                }
            }
        }.start(wait = false)
    }

    @AfterTest
    fun teardown() {
        engine?.stop(2000L, 3000L)
    }

    @Test
    @Disabled  //TODO 版本冲突
    fun testRoot() = runBlocking {
        val client = HttpClient()
        val response = client.get("http://localhost:$PORT/")
        assertEquals("Hello World!", response.bodyAsText())
    }

}