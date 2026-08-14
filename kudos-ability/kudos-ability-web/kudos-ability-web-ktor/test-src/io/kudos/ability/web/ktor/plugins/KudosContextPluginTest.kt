package io.kudos.ability.web.ktor.plugins

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.kudos.ability.web.common.trace.TraceKeys
import io.kudos.context.core.KudosContext
import io.kudos.context.support.Consts
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
        assertTrue(UUID_PATTERN.matches(body), "traceKey should be a UUID but was: $body")
    }

    private companion object {
        val UUID_PATTERN =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }

    /**
     * A caller-supplied trace key goes straight into log lines, so it must not be taken on trust: CR/LF in that
     * value forges log entries, and an unbounded length inflates any log or metrics backend that indexes by
     * trace id. This factory used to propagate the header verbatim, which made the very same request safe or
     * unsafe depending only on whether Spring MVC or Ktor happened to serve it.
     */
    @Test
    fun defaultFactory_rejectsUnsafeTraceKey_andGeneratesOne() = testApplication {
        application {
            install(KudosContextPlugin)
            routing {
                get("/") { call.respondText(call.kudosContext().traceKey ?: "") }
            }
        }

        // CR/LF is deliberately absent here: Ktor's own client refuses to transmit such a header value, so it
        // cannot be driven through this test — which is a client-side courtesy, not a server-side defence, and
        // says nothing about a caller that is not using Ktor. The rule itself is pinned in `TraceKeysTest`.
        val unsafe = listOf(
            "a".repeat(TraceKeys.DEFAULT_MAX_LENGTH + 1),
            "trace with spaces",
            "   "
        )
        unsafe.forEach { value ->
            val body = client.get("/") { header(TraceKeys.TRACE_ID_HEADER, value) }.bodyAsText()
            assertTrue(body != value, "Unsafe trace key should have been rejected: $value")
            assertTrue(UUID_PATTERN.matches(body), "Expected a generated UUID but was: $body")
        }
    }

    /** A well-formed caller trace key is the whole point of the header, so it must survive untouched. */
    @Test
    fun defaultFactory_acceptsWellFormedTraceKey() = testApplication {
        application {
            install(KudosContextPlugin)
            routing {
                get("/") { call.respondText(call.kudosContext().traceKey ?: "") }
            }
        }

        val body = client.get("/") { header(TraceKeys.TRACE_ID_HEADER, "abc-123_def.456") }.bodyAsText()
        assertEquals("abc-123_def.456", body)
    }

    /** The header list is configurable so a deployment can also continue internal Feign traces. */
    @Test
    fun configuredTraceKeyHeaders_areConsultedInOrder() = testApplication {
        application {
            install(KudosContextPlugin) {
                traceKeyHeaders = listOf(TraceKeys.TRACE_ID_HEADER, Consts.RequestHeader.TRACE_KEY)
            }
            routing {
                get("/") { call.respondText(call.kudosContext().traceKey ?: "") }
            }
        }

        // Public header wins when both are present.
        assertEquals(
            "trace-public",
            client.get("/") {
                header(TraceKeys.TRACE_ID_HEADER, "trace-public")
                header(Consts.RequestHeader.TRACE_KEY, "trace-internal")
            }.bodyAsText()
        )
        // A rejected value in one header must not discard a good value in the next.
        assertEquals(
            "trace-internal",
            client.get("/") {
                header(TraceKeys.TRACE_ID_HEADER, "bad value")
                header(Consts.RequestHeader.TRACE_KEY, "trace-internal")
            }.bodyAsText()
        )
    }

    /** The length bound is configurable, and the configured value is what the default factory enforces. */
    @Test
    fun configuredMaxTraceKeyLength_isEnforced() = testApplication {
        application {
            install(KudosContextPlugin) { maxTraceKeyLength = 8 }
            routing {
                get("/") { call.respondText(call.kudosContext().traceKey ?: "") }
            }
        }

        assertEquals("12345678", client.get("/") { header(TraceKeys.TRACE_ID_HEADER, "12345678") }.bodyAsText())
        val tooLong = client.get("/") { header(TraceKeys.TRACE_ID_HEADER, "123456789") }.bodyAsText()
        assertTrue(UUID_PATTERN.matches(tooLong), "Over-long key should have been replaced, got: $tooLong")
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
