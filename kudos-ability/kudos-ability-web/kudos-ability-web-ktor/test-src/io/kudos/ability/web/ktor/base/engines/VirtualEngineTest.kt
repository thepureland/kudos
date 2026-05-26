package io.kudos.ability.web.ktor.base.engines

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.kudos.ability.web.ktor.init.installPlugins
import io.kudos.test.common.init.EnableKudosTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for Ktor's built-in in-memory virtual engine.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
class VirtualEngineTest {

    @Test
    fun testRoot() = testApplication {
        application {
            installPlugins()
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