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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the installation paths of [KudosContextPlugin].
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

    @Test
    fun defaultFactory_generatesUuidTraceKey_whenHeaderMissing() = testApplication {
        application {
            install(KudosContextPlugin)
            routing {
                get("/") { call.respondText(call.kudosContext().traceKey ?: "") }
            }
        }

        // 不带 X-Trace-Id 请求头时，默认工厂应生成 UUID 作为 traceKey
        val body = client.get("/").bodyAsText()
        assertTrue(
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(body),
            "traceKey should be a UUID but was: $body"
        )
    }

    @Test
    fun kudosContextOrNull_returnsNull_whenPluginNotInstalled() = testApplication {
        application {
            routing {
                get("/") { call.respondText((call.kudosContextOrNull() == null).toString()) }
            }
        }

        assertEquals("true", client.get("/").bodyAsText())
    }

    @Test
    fun kudosContext_throws_whenPluginNotInstalled() = testApplication {
        application {
            routing {
                get("/") {
                    val ex = assertFailsWith<IllegalArgumentException> { call.kudosContext() }
                    call.respondText(ex.message ?: "")
                }
            }
        }

        assertEquals("KudosContext is absent in ApplicationCall.attributes", client.get("/").bodyAsText())
    }
}
