package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end tests for the [kudosWebSocket] route extension function.
 *
 * Uses `testApplication { ... }` ([io.ktor.server.testing.testApplication]) to spin up a
 * Ktor application in memory, mount the route, and connect back with a ktor client sharing
 * the same application context.
 *
 * Covers:
 *  - **Connection lifecycle**: register before onConnect; unregister after onDisconnect.
 *  - **Message send/receive**: client sends text → handler.onText receives it.
 *  - **Server-initiated send**: handler calls `session.sendText` → client incoming receives it.
 *  - **sessionFactory carries business metadata**: userId / tenantId are propagated to the registry.
 *  - **Graceful close**: the registry count returns to zero after the connection is closed.
 */
internal class KudosWebSocketRoutingTest {

    @Test
    fun roundtrip_clientSendsTextAndReceivesEcho() = testApplication {
        val registry = KudosWebSocketRegistry()
        val received = Channel<String>(capacity = 16)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSession, text: String) {
                received.send(text)
                session.sendText("echo:$text")
            }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket("/ws", registry, handler)
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            send(Frame.Text("hello"))
            val response = incoming.receive() as Frame.Text
            assertEquals("echo:hello", response.readText())
        }

        // Client connection is closed; wait for the server's finally block to remove the session from the registry.
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size, "Registry count should return to zero after a normal close")
        assertEquals("hello", received.tryReceive().getOrNull())
    }

    @Test
    fun sessionFactory_carriesUserAndTenantInfo() = testApplication {
        val registry = KudosWebSocketRegistry()
        val captured = Channel<Triple<String, String?, String?>>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSession) {
                captured.send(Triple(session.sessionId, session.userId, session.tenantId))
                session.close()
            }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket("/ws", registry, handler) { raw ->
                    KudosWebSocketSession(
                        raw = raw,
                        userId = raw.call.request.headers["X-User-Id"],
                        tenantId = raw.call.request.headers["X-Tenant-Id"],
                    )
                }
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket(
            urlString = "/ws",
            request = {
                headers.append("X-User-Id", "alice")
                headers.append("X-Tenant-Id", "tenant-1")
            },
        ) {
            // The server actively closes once onConnect completes; no need to send anything — just wait for the connection to close.
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
        }

        val info = captured.tryReceive().getOrNull()
        requireNotNull(info) { "onConnect should record one session" }
        assertTrue(info.first.isNotBlank())
        assertEquals("alice", info.second)
        assertEquals("tenant-1", info.third)

        waitFor { registry.size == 0 }
    }

    @Test
    fun onConnect_thenOnText_thenOnDisconnect_lifecycleOrder() = testApplication {
        val registry = KudosWebSocketRegistry()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSession) { order.add("connect:${session.sessionId}") }
            override suspend fun onText(session: KudosWebSocketSession, text: String) { order.add("text:$text") }
            override suspend fun onDisconnect(session: KudosWebSocketSession, cause: Throwable?) {
                // At this point the registry still holds the session (finally order: onDisconnect first, then unregister).
                order.add("disconnect:sizeBeforeUnregister=${registry.size}")
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            send(Frame.Text("hi"))
            // Give the server a moment to receive and process the frame.
            delay(100)
        }

        waitFor { registry.size == 0 }
        val snapshot = order.toList()
        // connect must come before text; text must come before disconnect.
        val connectIdx = snapshot.indexOfFirst { it.startsWith("connect:") }
        val textIdx = snapshot.indexOf("text:hi")
        val disconnectIdx = snapshot.indexOfFirst { it.startsWith("disconnect:") }
        assertTrue(connectIdx >= 0 && textIdx > connectIdx && disconnectIdx > textIdx,
            "Hook order should be connect -> text -> disconnect, actual: $snapshot")
        // During onDisconnect the registry should still see this session (unregister runs later in the finally block).
        assertEquals("disconnect:sizeBeforeUnregister=1",
            snapshot.first { it.startsWith("disconnect:") })
    }



    /** Simple polling wait — the unregister in the server's finally block happens asynchronously (coroutine yield). */
    private suspend fun waitFor(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            delay(20)
        }
        // Do not throw on timeout — the actual assertion is up to the caller; this function only best-effort waits.
    }
}
