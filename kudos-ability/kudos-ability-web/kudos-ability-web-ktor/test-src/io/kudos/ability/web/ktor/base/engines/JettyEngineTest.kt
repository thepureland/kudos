package io.kudos.ability.web.ktor.base.engines

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.kudos.ability.web.ktor.core.KtorContext
import io.kudos.test.common.init.EnableKudosTest
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
@EnableKudosTest(
    properties = ["kudos.ability.web.ktor.engine.name=jetty"]
)
@Disabled("jetty依赖有版本冲突")
class JettyEngineTest {

    @BeforeTest
    fun setup() {
        KtorContext.application.routing {
            get("/") {
                call.respondText("Hello Jetty!")
            }
        }
    }

    @AfterTest
    fun teardown() {
        KtorContext.application.engine.stop(2000L, 3000L)
    }

    @Test
    @Disabled  //TODO 版本冲突
    fun testRoot() = runBlocking {
        val client = HttpClient()
        val response = client.get("http://localhost:${KtorContext.properties.engine.port}/")
        assertEquals("Hello Jetty!", response.bodyAsText())
    }

}