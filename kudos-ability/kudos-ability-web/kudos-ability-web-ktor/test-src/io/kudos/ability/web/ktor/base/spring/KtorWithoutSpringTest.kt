package io.kudos.ability.web.ktor.base.spring

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.kudos.ability.web.ktor.base.init.KtorProperties
import io.kudos.ability.web.ktor.base.init.installPlugins
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ktor不整合Spring的测试用例
 *
 * @author K
 * @since 1.0.0
 */
class KtorWithoutSpringTest {

    @Test
    fun testRoot() = testApplication {
        application {
            installPlugins(KtorProperties())
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