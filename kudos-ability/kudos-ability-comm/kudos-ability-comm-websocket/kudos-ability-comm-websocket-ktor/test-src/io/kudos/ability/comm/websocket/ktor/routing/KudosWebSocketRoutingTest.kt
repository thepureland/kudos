package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.connect.WebSocketConnectDecision
import io.kudos.ability.comm.websocket.common.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.CloseReason
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
 *  - **sessionFactory rejection**: a null return closes the connection and never registers it.
 *  - **Graceful close**: the registry count returns to zero after the connection is closed.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosWebSocketRoutingTest {

    @Test
    fun roundtrip_clientSendsTextAndReceivesEcho() = testApplication {
        val registry = KudosWebSocketRegistry()
        val received = Channel<String>(capacity = 16)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
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
            override suspend fun onConnect(session: KudosWebSocketSessionRef) {
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
    fun sessionFactoryReturningNull_rejectsWithoutRegisteringOrInvokingHooks() = testApplication {
        val registry = KudosWebSocketRegistry()
        val hookCalls = AtomicInteger()
        val maxRegistrySize = AtomicInteger()
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) { hookCalls.incrementAndGet() }
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) { hookCalls.incrementAndGet() }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket("/ws", registry, handler) { raw ->
                    maxRegistrySize.updateAndGet { maxOf(it, registry.size) }
                    // Authentication failed: reject before the session can join a tenant's broadcast group.
                    if (raw.call.request.headers["X-User-Id"] == null) null else KudosWebSocketSession(raw)
                }
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        val closeCode = AtomicReference<Short?>()
        client.webSocket("/ws") {
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
            closeCode.set(closeReason.await()?.code)
        }

        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, closeCode.get(),
            "A rejected connection should be closed with a policy-violation reason")
        assertEquals(0, hookCalls.get(), "No handler hook may run for a connection that was never admitted")
        assertEquals(0, maxRegistrySize.get(), "A rejected connection must never appear in the registry")
        assertEquals(0, registry.size)
    }

    @Test
    fun connectInterceptors_runInOrder_andFirstRejectionWins() = testApplication {
        val registry = KudosWebSocketRegistry()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val hookCalls = AtomicInteger()
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) { hookCalls.incrementAndGet() }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket(
                    "/ws", registry, handler,
                    connectInterceptors = listOf(
                        IWebSocketConnectInterceptor { order.add("first"); WebSocketConnectDecision.Proceed },
                        IWebSocketConnectInterceptor {
                            order.add("second")
                            WebSocketConnectDecision.Reject(WebSocketCloseReason.Codes.TRY_AGAIN_LATER, "full")
                        },
                        IWebSocketConnectInterceptor { order.add("third"); WebSocketConnectDecision.Proceed },
                    ),
                )
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        val closeCode = AtomicReference<Short?>()
        client.webSocket("/ws") {
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
            closeCode.set(closeReason.await()?.code)
        }

        assertEquals(listOf("first", "second"), order.toList(),
            "Interceptors run in order and stop at the first rejection")
        assertEquals(CloseReason.Codes.TRY_AGAIN_LATER.code, closeCode.get(),
            "The rejecting interceptor's own CloseReason must reach the client")
        assertEquals(0, hookCalls.get(), "A rejected connection must not reach any handler hook")
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size, "A rejected connection must never enter the registry")
    }

    @Test
    fun rejectedConnection_isNeverReachableByBroadcast() = testApplication {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val reachedDuringAdmission = AtomicInteger(-1)

        application {
            install(WebSockets)
            routing {
                kudosWebSocket(
                    "/ws", registry, object : IKudosWebSocketHandler {},
                    connectInterceptors = listOf(
                        IWebSocketConnectInterceptor {
                            // The whole point of rejecting here rather than inside onConnect: at this
                            // moment the session claims tenant "acme" but must not yet be a member of it.
                            reachedDuringAdmission.set(broadcaster.broadcastToTenant("acme", "secret"))
                            WebSocketConnectDecision.Reject(WebSocketCloseReason.Codes.VIOLATED_POLICY, "not allowed")
                        },
                    ),
                ) { raw -> KudosWebSocketSession(raw, userId = "mallory", tenantId = "acme") }
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
        }

        assertEquals(0, reachedDuringAdmission.get(),
            "A tenant broadcast during admission must not reach the not-yet-admitted session")
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size)
    }

    @Test
    fun throwingConnectInterceptor_failsClosed() = testApplication {
        val registry = KudosWebSocketRegistry()
        val hookCalls = AtomicInteger()
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) { hookCalls.incrementAndGet() }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket(
                    "/ws", registry, handler,
                    connectInterceptors = listOf(
                        IWebSocketConnectInterceptor { error("quota backend unreachable") },
                    ),
                )
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
        }

        assertEquals(0, hookCalls.get(),
            "An admission check that blew up must not be read as 'allowed' — it fails closed")
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size)
    }

    @Test
    fun noConnectInterceptors_admitsAsBefore() = testApplication {
        val registry = KudosWebSocketRegistry()
        val connected = Channel<Unit>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) { connected.send(Unit) }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") { connected.receive() }

        waitFor { registry.size == 0 }
    }

    @Test
    fun onConnect_thenOnText_thenOnDisconnect_lifecycleOrder() = testApplication {
        val registry = KudosWebSocketRegistry()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) { order.add("connect:${session.sessionId}") }
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) { order.add("text:$text") }
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
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

    @Test
    fun binaryRoundtrip_clientSendsBytesAndReceivesThemBack() = testApplication {
        val registry = KudosWebSocketRegistry()
        val receivedOnServer = Channel<ByteArray>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {
                receivedOnServer.send(bytes)
                session.sendBinary(bytes.reversedArray())
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val payload = byteArrayOf(1, 2, 3, -4, 5)
        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            send(Frame.Binary(true, payload))
            val response = incoming.receive() as Frame.Binary
            assertContentEquals(byteArrayOf(5, -4, 3, 2, 1), response.data)
        }

        assertContentEquals(payload, receivedOnServer.tryReceive().getOrNull())
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size)
    }

    @Test
    fun handlerException_isCaught_propagatedToOnDisconnectCause_andSessionUnregistered() = testApplication {
        val registry = KudosWebSocketRegistry()
        val disconnectCause = AtomicReference<Throwable?>()
        val disconnected = Channel<Unit>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                throw IllegalStateException("business handler blew up on: $text")
            }
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
                disconnectCause.set(cause)
                disconnected.send(Unit)
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            send(Frame.Text("boom"))
            // The server route lambda dies with the handler exception; wait for the connection to drop.
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
        }

        disconnected.receive()
        val cause = disconnectCause.get()
        assertNotNull(cause, "An abnormal disconnect must carry the original handler exception")
        assertEquals("business handler blew up on: boom", cause.message)
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size, "The session must be unregistered even after a handler exception")
    }

    @Test
    fun onDisconnectThrowing_doesNotPreventUnregister() = testApplication {
        val registry = KudosWebSocketRegistry()
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
                error("cleanup failed")
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") { /* connect, then close immediately */ }

        waitFor { registry.size == 0 }
        assertEquals(0, registry.size, "unregister runs even when onDisconnect throws (runCatching guard)")
    }

    @Test
    fun onDisconnectSuspending_completesEvenWhenTheRouteCoroutineIsCancelled() = testApplication {
        val registry = KudosWebSocketRegistry()
        val cleanupFinished = Channel<Boolean>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) {
                // Cancelling from inside the route models what a graceful shutdown does to the
                // session coroutine: everything after the first suspension point in cleanup would
                // be skipped without the NonCancellable guard.
                throw kotlinx.coroutines.CancellationException("server going away")
            }
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
                delay(20) // a suspending cleanup step: clearing a presence key, writing an audit row
                cleanupFinished.send(true)
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            try { incoming.receive() } catch (_: Throwable) { /* expected close */ }
        }

        assertEquals(true, cleanupFinished.receive(),
            "Suspending cleanup must run to completion under cancellation, not abort at its first delay")
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size)
    }

    @Test
    fun explicitClientCloseFrame_endsLoopWithNullCause() = testApplication {
        val registry = KudosWebSocketRegistry()
        val disconnectCause = AtomicReference<Throwable?>(RuntimeException("sentinel: not yet called"))
        val disconnected = Channel<Unit>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
                disconnectCause.set(cause)
                disconnected.send(Unit)
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            close(CloseReason(CloseReason.Codes.NORMAL, "bye"))
        }

        disconnected.receive()
        assertEquals(null, disconnectCause.get(), "A client-initiated close is a normal disconnect (cause = null)")
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size)
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
