package io.kudos.ability.comm.websocket.spring.handler

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.connect.WebSocketConnectDecision
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import io.kudos.ability.comm.websocket.spring.FakeWebSocketSession
import io.kudos.ability.comm.websocket.spring.session.SpringWebSocketSession
import kotlinx.coroutines.delay
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Unit tests for [KudosSpringWebSocketHandler] — the bridge from Spring's blocking, one-shot handler
 * callbacks onto the `suspend` [IKudosWebSocketHandler] SPI.
 *
 * The behaviours pinned down here are the ones the bridge exists to get right and that a naive
 * implementation silently breaks: **message ordering**, **register/unregister always happening**,
 * **rejection before registration**, and **an abnormal disconnect being reported as one**.
 *
 * Tests drive the handler through the exact callback sequence a servlet container would, and poll for
 * the pump coroutine to catch up — the pump is asynchronous by design, so there is no callback to
 * await instead.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosSpringWebSocketHandlerTest {

    private val registry = KudosWebSocketRegistry()

    @Test
    fun messages_reachTheHandlerInOrder() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        val expected = (1..200).map { "m$it" }
        expected.forEach { handler.handleMessage(raw, TextMessage(it)) }
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)

        awaitUntil { business.disconnected }
        // A launch-per-message bridge passes "every message arrived" and fails exactly this assertion.
        assertEquals(expected, business.texts.toList(), "A single-consumer pump must preserve message order")
        handler.destroy()
    }

    @Test
    fun session_isRegisteredWhileConnectedAndUnregisteredAfterwards() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession(sessionId = "abc")

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        assertEquals(1, registry.size)
        assertNotNull(registry.findById("abc"))

        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)
        awaitUntil { business.disconnected }
        assertEquals(0, registry.size, "Teardown must always unregister, or the registry fills with dead sessions")
        handler.destroy()
    }

    @Test
    fun queuedMessages_areStillProcessedAfterTheConnectionCloses() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        handler.handleMessage(raw, TextMessage("last words"))
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)

        awaitUntil { business.disconnected }
        assertEquals(listOf("last words"), business.texts.toList(),
            "Closing the queue must drain it; cancelling would drop a message the client did send")
        handler.destroy()
    }

    @Test
    fun binaryMessages_reachOnBinary() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()
        val bytes = byteArrayOf(1, 2, 3, -1)

        handler.afterConnectionEstablished(raw)
        handler.handleMessage(raw, BinaryMessage(ByteBuffer.wrap(bytes)))
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)

        awaitUntil { business.disconnected }
        assertContentEquals(bytes, business.binaries.single())
        handler.destroy()
    }

    @Test
    fun pingAndPong_neverReachBusinessCode() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        handler.handleMessage(raw, PingMessage())
        handler.handleMessage(raw, PongMessage())
        handler.handleMessage(raw, TextMessage("real"))
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)

        awaitUntil { business.disconnected }
        assertEquals(listOf("real"), business.texts.toList(),
            "The container answers Ping itself; surfacing control frames would make every handler write the same empty branch")
        handler.destroy()
    }

    @Test
    fun rejectingSessionFactory_neverRegisters_andClosesTheConnection() {
        val business = RecordingHandler()
        val handler = newHandler(business, sessionFactory = { null })
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)

        awaitUntil { raw.closedWith != null }
        assertEquals(0, registry.size)
        assertEquals(WebSocketCloseReason.REJECTED.code.toInt(), raw.closedWith?.code)
        assertTrue(!business.connected, "A rejected connection must never reach onConnect")
        handler.destroy()
    }

    /**
     * Rejecting *before* registration is the whole point of the admission seam: a session that is
     * registered first and closed later is, for that window, a member of its claimed tenant's
     * broadcast group.
     */
    @Test
    fun rejectingConnectInterceptor_neverRegisters() {
        val business = RecordingHandler()
        val handler = newHandler(
            business,
            interceptors = listOf(IWebSocketConnectInterceptor {
                WebSocketConnectDecision.Reject(WebSocketCloseReason.Codes.TRY_AGAIN_LATER, "full")
            }),
        )
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)

        awaitUntil { raw.closedWith != null }
        assertEquals(0, registry.size)
        assertEquals(1013, raw.closedWith?.code, "The interceptor's own close reason must reach the client")
        assertTrue(!business.connected)
        handler.destroy()
    }

    @Test
    fun aThrowingConnectInterceptor_failsClosed() {
        val business = RecordingHandler()
        val handler = newHandler(
            business,
            interceptors = listOf(IWebSocketConnectInterceptor { error("quota lookup exploded") }),
        )
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)

        awaitUntil { raw.closedWith != null }
        assertEquals(0, registry.size, "A check that blew up must not be read as 'allowed'")
        handler.destroy()
    }

    @Test
    fun normalClose_reportsNoCause() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)

        awaitUntil { business.disconnected }
        assertNull(business.disconnectCause, "A clean close is not an error")
        handler.destroy()
    }

    /**
     * A client that vanishes — laptop lid closed, cellular handover — yields close code 1006 and *no*
     * transport error. Reporting only the transport error would tell business code that a dropped
     * connection closed cleanly, which is exactly backwards for a presence flag.
     */
    @Test
    fun abnormalClose_withoutATransportError_stillReportsACause() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        handler.afterConnectionClosed(raw, CloseStatus.NO_CLOSE_FRAME) // 1006

        awaitUntil { business.disconnected }
        val cause = assertIs<WebSocketAbnormalCloseException>(business.disconnectCause)
        assertEquals(1006, cause.closeStatus.code)
        handler.destroy()
    }

    @Test
    fun aTransportError_isReportedAsTheDisconnectCause() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()
        val boom = IllegalStateException("connection reset")

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        handler.handleTransportError(raw, boom)
        handler.afterConnectionClosed(raw, CloseStatus.NO_CLOSE_FRAME)

        awaitUntil { business.disconnected }
        assertEquals(boom, business.disconnectCause, "The real error beats the synthetic one")
        handler.destroy()
    }

    @Test
    fun aThrowingBusinessHandler_stillUnregisters() {
        val business = object : RecordingHandler() {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                super.onText(session, text)
                error("business blew up")
            }
        }
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        handler.handleMessage(raw, TextMessage("boom"))

        awaitUntil { business.disconnected }
        assertEquals(0, registry.size)
        assertEquals("business blew up", business.disconnectCause?.message)
        handler.destroy()
    }

    @Test
    fun dropLatestPolicy_neverBlocksTheContainerThread() {
        val gate = CountDownLatch(1)
        val business = object : RecordingHandler() {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                super.onText(session, text)
                gate.await(5, TimeUnit.SECONDS) // pump is stuck; the queue backs up behind it
            }
        }
        val handler = newHandler(
            business,
            options = KudosSpringWebSocketHandler.Options(
                inboundBufferCapacity = 2,
                inboundOverflow = InboundOverflowPolicy.DROP_LATEST,
            ),
        )
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        handler.handleMessage(raw, TextMessage("stuck"))
        awaitUntil { business.texts.isNotEmpty() }

        val startedAt = System.nanoTime()
        repeat(50) { handler.handleMessage(raw, TextMessage("flood-$it")) }
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue(elapsedMillis < 2_000,
            "DROP_LATEST must shed load rather than hold the container thread; took $elapsedMillis ms")
        gate.countDown()
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)
        awaitUntil { business.disconnected }
        handler.destroy()
    }

    /**
     * The default policy's whole justification is that it loses nothing. A queue far smaller than the
     * burst, drained by a deliberately slow handler, is the case where a dropping implementation would
     * quietly lose messages and still pass every other test in this file.
     */
    @Test
    fun backpressurePolicy_losesNothingWhenTheQueueOverflows() {
        val business = object : RecordingHandler() {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                super.onText(session, text)
                delay(2) // slower than the producer below, so the queue is permanently full
            }
        }
        val handler = newHandler(
            business,
            options = KudosSpringWebSocketHandler.Options(
                inboundBufferCapacity = 2,
                inboundOverflow = InboundOverflowPolicy.BACKPRESSURE,
            ),
        )
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        val expected = (1..40).map { "m$it" }
        expected.forEach { handler.handleMessage(raw, TextMessage(it)) }
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)

        awaitUntil { business.disconnected }
        assertEquals(expected, business.texts.toList(),
            "BACKPRESSURE must block the producer, not drop — and blocking must not reorder either")
        handler.destroy()
    }

    @Test
    fun closePolicy_disconnectsTheFloodingClient() {
        val gate = CountDownLatch(1)
        val business = object : RecordingHandler() {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                super.onText(session, text)
                gate.await(5, TimeUnit.SECONDS)
            }
        }
        val handler = newHandler(
            business,
            options = KudosSpringWebSocketHandler.Options(
                inboundBufferCapacity = 2,
                inboundOverflow = InboundOverflowPolicy.CLOSE,
            ),
        )
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }
        repeat(20) { handler.handleMessage(raw, TextMessage("flood-$it")) }

        assertEquals(CloseStatus.SESSION_NOT_RELIABLE.code, raw.closedWith?.code,
            "CLOSE treats a client outrunning the server as abuse, not as load to absorb")
        gate.countDown()
        handler.afterConnectionClosed(raw, CloseStatus.NO_CLOSE_FRAME)
        awaitUntil { business.disconnected }
        handler.destroy()
    }

    /**
     * The entire "the container reassembles fragments for us" design rests on this returning `false`.
     * Flipping it to `true` would hand business code half a message — and split any multi-byte UTF-8
     * character straddling the boundary — with nothing else in the module to catch it.
     */
    @Test
    fun partialMessages_areNotSupported() {
        val handler = newHandler(RecordingHandler())

        assertFalse(handler.supportsPartialMessages())
        handler.destroy()
    }

    /**
     * Without the decorator, a broadcast and a reply landing on the same connection at the same moment
     * throw `IllegalStateException: The remote endpoint was in state [TEXT_FULL_WRITING]` — a failure
     * that only appears under concurrent fan-out, i.e. in production rather than in any test that sends
     * one message at a time. Asserting the wrapping is the cheap way to keep it.
     */
    @Test
    fun theSessionHandedToTheFactory_isWrappedForConcurrentSends() {
        val seen = java.util.concurrent.atomic.AtomicReference<org.springframework.web.socket.WebSocketSession>()
        val business = RecordingHandler()
        val handler = newHandler(business, sessionFactory = { raw ->
            seen.set(raw)
            SpringWebSocketSession(raw)
        })
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }

        assertIs<ConcurrentWebSocketSessionDecorator>(seen.get())
        handler.destroy()
    }

    @Test
    fun activeConnections_tracksLiveSessions() {
        val handler = newHandler(RecordingHandler())
        val a = FakeWebSocketSession(sessionId = "a")
        val b = FakeWebSocketSession(sessionId = "b")

        handler.afterConnectionEstablished(a)
        handler.afterConnectionEstablished(b)
        assertEquals(2, handler.activeConnections)

        handler.afterConnectionClosed(a, CloseStatus.NORMAL)
        assertEquals(1, handler.activeConnections)
        handler.destroy()
    }

    /**
     * Without this, a context close leaves pumps mid-message and skips `onDisconnect` on every rolling
     * restart — precisely when the whole fleet does it at once and presence state matters most.
     */
    @Test
    fun destroy_runsDisconnectCleanupForStillLiveSessions() {
        val business = RecordingHandler()
        val handler = newHandler(business)
        val raw = FakeWebSocketSession()

        handler.afterConnectionEstablished(raw)
        awaitUntil { business.connected }

        handler.destroy()

        assertTrue(business.disconnected, "destroy() must drain pumps, not just cancel them")
        assertEquals(0, registry.size)
    }

    private fun newHandler(
        business: IKudosWebSocketHandler,
        sessionFactory: suspend (org.springframework.web.socket.WebSocketSession) -> SpringWebSocketSession? =
            { SpringWebSocketSession(it, userId = "u1", tenantId = "t1") },
        interceptors: List<IWebSocketConnectInterceptor> = emptyList(),
        options: KudosSpringWebSocketHandler.Options = KudosSpringWebSocketHandler.Options(),
    ) = KudosSpringWebSocketHandler(
        registry = registry,
        handler = business,
        sessionFactory = { sessionFactory(it) },
        connectInterceptors = interceptors,
        options = options,
    )

    /** Records every hook invocation; the pump is asynchronous, so tests poll these flags. */
    private open class RecordingHandler : IKudosWebSocketHandler {
        val texts = CopyOnWriteArrayList<String>()
        val binaries = CopyOnWriteArrayList<ByteArray>()

        @Volatile
        var connected = false

        @Volatile
        var disconnected = false

        @Volatile
        var disconnectCause: Throwable? = null

        override suspend fun onConnect(session: KudosWebSocketSessionRef) { connected = true }

        override suspend fun onText(session: KudosWebSocketSessionRef, text: String) { texts += text }

        override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) { binaries += bytes }

        override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
            disconnectCause = cause
            disconnected = true
        }
    }

    /** Polls [condition] until it holds. The pump has no completion callback to await instead. */
    private fun awaitUntil(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000
        while (System.nanoTime() < deadlineNanos) {
            if (condition()) return
            Thread.sleep(5)
        }
        fail("Condition was not met within $timeoutMillis ms")
    }
}
