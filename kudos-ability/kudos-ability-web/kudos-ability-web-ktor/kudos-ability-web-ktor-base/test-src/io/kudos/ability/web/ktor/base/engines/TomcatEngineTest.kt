package io.kudos.ability.web.ktor.base.engines

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.tomcat.jakarta.*
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * tomcat引擎ktor测试
 *
 * @author K
 * @since 1.0.0
 */
class TomcatEngineTest {

    private val PORT = 9593
    private var engine: EmbeddedServer<TomcatApplicationEngine, *>? = null

    @BeforeTest
    fun setup() {
        engine = embeddedServer(Tomcat, PORT) {
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
    fun testRoot() = runBlocking {
        val client = HttpClient()
        val response = client.get("http://localhost:$PORT/")
        assertEquals("Hello World!", response.bodyAsText())
    }

}