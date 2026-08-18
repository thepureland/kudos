package io.kudos.ability.comm.websocket.spring.server

import io.kudos.ability.comm.websocket.common.broadcast.IWebSocketBroadcaster
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.test.common.init.EnableKudosTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end tests: a real embedded Tomcat, a real WebSocket handshake, a real client.
 *
 * Everything else in this module is unit-level, which leaves a specific class of defect uncovered —
 * **wiring that only exists at runtime**. None of the following can fail a unit test, and all of them
 * would break a deployment:
 *
 *  - `@EnableWebSocket` + `WebSocketConfigurer` not actually mounting the endpoints
 *  - `IKudosWebSocketEndpoint` beans not reaching the registry
 *  - the `ServletServerContainerFactoryBean` not applying, leaving the Jakarta 8 KB buffer in place
 *  - the registry / broadcaster beans from the common module not being visible to this one
 *  - a handshake guard rejecting in a way the container turns into something other than an HTTP status
 *
 * The suite deliberately asserts through the client — what a browser would observe — rather than
 * through server-side state where a unit test already suffices.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(WebSocketTestEndpoints::class)
internal class SpringWebSocketEndToEndTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var registry: KudosWebSocketRegistry

    @Autowired
    private lateinit var broadcaster: IWebSocketBroadcaster

    @Autowired
    private lateinit var echoHandler: RecordingTestHandler

    @Autowired
    private lateinit var sockJsHandler: RecordingTestHandler

    @Test
    fun aRealHandshakeUpgradesAndEchoesBack() {
        val (session, client) = CollectingClient.connect(port, "${WebSocketTestEndpoints.ECHO_PATH}?token=alice")

        session.sendMessage(org.springframework.web.socket.TextMessage("hello"))

        assertEquals("echo:hello", client.awaitText())
        // `contains`, not `poll`: the handler is a singleton shared by every test in this class, so the
        // queue also holds the userIds of connections other tests opened.
        awaitUntil { echoHandler.connectedUserIds.contains("alice") }
        session.close()
    }

    /**
     * The Jakarta WebSocket default text buffer is 8 KB, and `supportsPartialMessages = false` means the
     * container — not this module — decides the largest message an endpoint can accept. A 32 KB round
     * trip is the only way to prove `ServletServerContainerFactoryBean` actually took effect.
     */
    @Test
    fun aMessageLargerThanTheJakartaDefaultBuffer_survivesIntact() {
        val (session, client) = CollectingClient.connect(port, "${WebSocketTestEndpoints.ECHO_PATH}?token=bob")
        val payload = "x".repeat(32 * 1024)

        session.sendMessage(org.springframework.web.socket.TextMessage(payload))

        assertEquals("echo:$payload", client.awaitText(10))
        session.close()
    }

    @Test
    fun binaryMessages_roundTrip() {
        val (session, client) = CollectingClient.connect(port, "${WebSocketTestEndpoints.ECHO_PATH}?token=carol")
        val bytes = ByteArray(1024) { (it % 256 - 128).toByte() }

        session.sendMessage(org.springframework.web.socket.BinaryMessage(bytes))

        assertContentEquals(bytes, client.awaitBinary())
        session.close()
    }

    /**
     * Proves the whole cross-module chain at once: the session factory tagged the connection with a
     * userId, the registry from `kudos-ability-comm-websocket-common` indexed it, and the broadcaster
     * bean resolved that index back to a live socket.
     */
    @Test
    fun aServerSideBroadcastReachesTheConnectedClient() {
        val (session, client) = CollectingClient.connect(port, "${WebSocketTestEndpoints.ECHO_PATH}?token=dave")
        awaitUntil { registry.findByUserId("dave").isNotEmpty() }

        val reached = runBlocking { broadcaster.broadcastToUser("dave", "server push") }

        assertEquals(1, reached)
        assertEquals("server push", client.awaitText())
        session.close()
    }

    @Test
    fun theRegistryTracksTheConnectionForItsWholeLifetime() {
        val (session, _) = CollectingClient.connect(port, "${WebSocketTestEndpoints.ECHO_PATH}?token=erin")

        awaitUntil { registry.findByUserId("erin").isNotEmpty() }
        val tracked = registry.findByUserId("erin").single()
        assertEquals("t1", tracked.tenantId)

        session.close()

        awaitUntil { registry.findByUserId("erin").isEmpty() }
    }

    @Test
    fun aClientInitiatedClose_isReportedAsACleanDisconnect() {
        val (session, _) = CollectingClient.connect(port, "${WebSocketTestEndpoints.ECHO_PATH}?token=frank")
        awaitUntil { registry.findByUserId("frank").isNotEmpty() }
        val sessionId = registry.findByUserId("frank").single().sessionId

        session.close()

        awaitUntil { echoHandler.disconnectHadCause.containsKey(sessionId) }
        assertEquals(false, echoHandler.disconnectHadCause[sessionId],
            "A client closing normally must not be reported to business code as an abnormal disconnect")
    }

    /**
     * The reason handshake guards exist. A rejection here is an HTTP status the client can read and act
     * on; rejecting later instead gives a socket that opens and immediately closes, which every
     * WebSocket client treats as a transient fault and retries — turning a bad credential into a
     * reconnect loop.
     */
    @Test
    fun aGuardRejection_failsTheUpgradeWithAnHttpStatus() {
        val failure = CollectingClient.connectExpectingFailure(port, WebSocketTestEndpoints.GUARDED_PATH)

        assertTrue(failure.messageChain().contains("401"),
            "Expected a 401 from the guard, got: ${failure.messageChain()}")
    }

    @Test
    fun aGuardThatPasses_letsTheUpgradeThrough() {
        val (session, client) = CollectingClient.connect(port, "${WebSocketTestEndpoints.GUARDED_PATH}?token=gina")

        session.sendMessage(org.springframework.web.socket.TextMessage("ok"))

        assertEquals("echo:ok", client.awaitText())
        session.close()
    }

    /**
     * An unidentifiable connection is refused *after* the upgrade — the seam the guard above exists to
     * avoid — so what the client observes is a close with the policy code, and the registry never held it.
     */
    @Test
    fun anUnidentifiableConnection_isClosedAndNeverRegistered() {
        val sizeBefore = registry.size
        val (_, client) = CollectingClient.connect(port, WebSocketTestEndpoints.REJECT_PATH)

        val closeStatus = client.awaitClose()

        assertEquals(1008, closeStatus.code, "The rejection close reason must reach the client")
        assertEquals(sizeBefore, registry.size, "A refused connection must never enter the registry")
    }

    @Test
    fun aSecondEndpointIsMountedIndependently() {
        val (session, client) = CollectingClient.connect(port, "${WebSocketTestEndpoints.NOTIFY_PATH}?token=henry")

        session.sendMessage(org.springframework.web.socket.TextMessage("ping"))

        assertEquals("echo:ping", client.awaitText())
        assertTrue(echoHandler.texts.none { it == "ping" },
            "Each endpoint must drive its own handler, not share one")
        session.close()
    }

    /**
     * SockJS is opt-in per endpoint, and turning it on mounts a whole extra URL surface (`/info`,
     * `/{server}/{session}/websocket`) that a unit test asserting `withSockJS()` was called cannot prove
     * actually works. This drives the real `SockJsClient` through that negotiation.
     */
    @Test
    fun aSockJsEndpoint_negotiatesAndCarriesMessages() {
        val (session, client) = CollectingClient.connectViaSockJs(port, WebSocketTestEndpoints.SOCKJS_PATH)

        session.sendMessage(org.springframework.web.socket.TextMessage("over sockjs"))

        assertEquals("echo:over sockjs", client.awaitText(10))
        session.close()
    }


    /**
     * The SockJS endpoint deliberately keeps the **default** session factory, so this also proves the
     * documented default: an endpoint that declares no identification accepts anonymous connections —
     * reachable by `broadcast` and `unicast`, but in neither the user nor the tenant index.
     */
    @Test
    fun anEndpointWithoutASessionFactory_acceptsAnonymousConnections() {
        val (session, _) = CollectingClient.connectViaSockJs(port, WebSocketTestEndpoints.SOCKJS_PATH)

        awaitUntil { sockJsHandler.connectedUserIds.contains(RecordingTestHandler.ANONYMOUS) }
        val anonymous = registry.all().filter { it.userId == null }
        assertTrue(anonymous.isNotEmpty(), "The anonymous session must still be registered")
        assertTrue(anonymous.all { it.tenantId == null },
            "No identification means neither index applies — such a session is reachable only by broadcast/unicast")

        session.close()
    }

    /**
     * The security default this module deliberately keeps: with `allowed-origins` unset, Spring restricts
     * the endpoint to same-origin. Any page on the internet being able to open an authenticated socket as
     * a logged-in user is the WebSocket form of CSRF, and there is no preflight to stop it.
     */
    @Test
    fun aForeignOrigin_isRefusedByDefault() {
        val failure = CollectingClient.connectExpectingFailure(
            port, "${WebSocketTestEndpoints.ECHO_PATH}?token=mallory", origin = "http://evil.example.com",
        )

        assertTrue(failure.messageChain().contains("403"),
            "Expected a 403 for a cross-origin upgrade, got: ${failure.messageChain()}")
    }

    private fun awaitUntil(timeoutSeconds: Long = 5, condition: () -> Boolean) {
        val deadlineNanos = System.nanoTime() + timeoutSeconds * 1_000_000_000
        while (System.nanoTime() < deadlineNanos) {
            if (condition()) return
            Thread.sleep(10)
        }
        assertTrue(condition(), "Condition was not met within ${timeoutSeconds}s")
    }
}

/** Flattens the cause chain's messages — the container wraps the HTTP status a few layers deep. */
internal fun Throwable.messageChain(): String =
    generateSequence(this) { it.cause }.mapNotNull { "${it::class.simpleName}: ${it.message}" }.joinToString(" | ")

