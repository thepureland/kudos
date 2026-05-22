package io.kudos.ability.web.ktor.plugins

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.kudos.context.core.KudosContext
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [KudosContextPlugin] 安装路径测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class KudosContextPluginTest {

    @Test
    fun pluginInstalls_andHandlerStillRuns() = testApplication {
        application {
            install(KudosContextPlugin)
            routing {
                get("/") { call.respondText("hello") }
            }
        }

        val response = client.get("/") { header("X-Trace-Id", "trace-abc") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("hello", response.bodyAsText())
    }

    @Test
    fun customFactory_isInvokedPerCall() = testApplication {
        var factoryCalls = 0
        application {
            install(KudosContextPlugin) {
                factory = { _ ->
                    factoryCalls++
                    KudosContext().apply { traceKey = "fixed-key" }
                }
            }
            routing {
                get("/") { call.respondText("ok") }
            }
        }

        client.get("/")
        client.get("/")
        assertEquals(2, factoryCalls)
    }

    @Test
    fun handlerReadsContextFromApplicationCallAttributes() = testApplication {
        application {
            install(KudosContextPlugin)
            routing {
                get("/") {
                    call.respondText(requireNotNull(call.kudosContext().traceKey))
                }
            }
        }

        val response = client.get("/") { header("X-Trace-Id", "trace-from-header") }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("trace-from-header", response.bodyAsText())
    }
}
