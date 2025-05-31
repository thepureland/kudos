package io.kudos.ability.web.ktor.base.spring

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.kudos.ability.web.ktor.base.init.KtorAutoConfiguration
import io.kudos.ability.web.ktor.base.init.installPlugins
import io.kudos.context.kit.SpringKit
import io.kudos.test.common.init.EnableKudosTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Ktor整合Spring的测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
class KtorSpringTest {

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

        assertNotNull(SpringKit.getBean(KtorAutoConfiguration::class))
    }

}