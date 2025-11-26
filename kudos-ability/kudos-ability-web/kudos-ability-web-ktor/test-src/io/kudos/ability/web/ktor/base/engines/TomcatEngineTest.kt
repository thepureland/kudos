package io.kudos.ability.web.ktor.base.engines

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.kudos.ability.web.ktor.core.KtorContext
import io.kudos.test.common.init.EnableKudosTest
import kotlinx.coroutines.runBlocking
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
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
@EnableKudosTest(
    properties = ["kudos.ability.web.ktor.engine.name=tomcat"]
)
class TomcatEngineTest {

    companion object {

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.web.ktor.engine.name") { "tomcat" }
        }

    }

    @BeforeTest
    fun setup() {
        KtorContext.application.routing {
            get("/") {
                call.respondText("Hello Tomcat!")
            }
        }
    }

    @AfterTest
    fun teardown() {
        KtorContext.application.engine.stop(2000L, 3000L)
    }

    @Test
    fun testRoot() = runBlocking {
        val client = HttpClient()
        val response = client.get("http://localhost:${KtorContext.properties.engine.port}/")
        assertEquals("Hello Tomcat!", response.bodyAsText())
    }

}