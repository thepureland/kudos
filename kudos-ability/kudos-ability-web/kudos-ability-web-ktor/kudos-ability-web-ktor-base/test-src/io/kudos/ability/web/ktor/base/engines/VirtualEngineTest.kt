package io.kudos.ability.web.ktor.base.engines

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ktor自带内存虚拟引擎测试
 *
 * @author K
 * @since 1.0.0
 */
class VirtualEngineTest {

    @Test
    fun testRoot() = testApplication {
        application {
            routing {
                get("/") {
                    call.respondText("Hello World!")
                }
            }
        }

        val client = createClient {}

        val response = client.get("/")
        assertEquals("Hello World!", response.bodyAsText())
    }

}