package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.utils.io.InternalAPI
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [KudosWebSocketSession] without a running Ktor engine — the raw
 * [DefaultWebSocketServerSession] is a hand-written fake whose `outgoing` is a real channel,
 * so the frames produced by the wrapper can be inspected directly.
 *
 * A Mockito mock is deliberately **not** used here: in Ktor 3.x `WebSocketSession.send(Frame)`
 * is an interface *default member* (delegating to `outgoing`), and Mockito stubs default
 * methods to no-ops — frames would silently never reach `outgoing` and `receive()` would hang
 * forever. The Kotlin fake inherits the real default `send`, exercising the production path.
 *
 * Covers:
 * - constructor defaults: random non-blank UUID sessionId (unique per instance), null
 *   userId / tenantId, mutable concurrent attributes map;
 * - sendText wraps the text into a [Frame.Text] (including Unicode / empty payloads);
 * - sendBinary wraps the bytes into a final [Frame.Binary];
 * - close emits a [Frame.Close] carrying the given reason, and the interface default
 *   reason is `NORMAL` with an empty message.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosWebSocketSessionTest {

    @Test
    fun defaults_uuidSessionId_nullIdentity_emptyAttributes() {
        val raw = FakeRawSession()
        val s1 = KudosWebSocketSession(raw)
        val s2 = KudosWebSocketSession(raw)

        assertTrue(s1.sessionId.isNotBlank())
        assertNotEquals(s1.sessionId, s2.sessionId, "Each session gets its own generated id")
        assertNull(s1.userId)
        assertNull(s1.tenantId)
        assertTrue(s1.attributes.isEmpty())
        assertSame(raw, s1.raw)

        // The attributes map is an open extension point and must be mutable.
        s1.attributes["device"] = "ios"
        assertEquals("ios", s1.attributes["device"])
    }

    @Test
    fun explicitConstructorArguments_areExposedAsIs() {
        val s = KudosWebSocketSession(FakeRawSession(), sessionId = "sid-1", userId = "u-1", tenantId = "t-1")

        assertEquals("sid-1", s.sessionId)
        assertEquals("u-1", s.userId)
        assertEquals("t-1", s.tenantId)
    }

    @Test
    fun sendText_emitsTextFrameWithVerbatimContent() = runBlocking {
        val raw = FakeRawSession()
        val s = KudosWebSocketSession(raw)

        s.sendText("héllo 中文🚀")
        s.sendText("")

        val first = raw.sentFrames.receive()
        assertTrue(first is Frame.Text)
        assertEquals("héllo 中文🚀", first.readText())
        val second = raw.sentFrames.receive()
        assertTrue(second is Frame.Text)
        assertEquals("", second.readText())
    }

    @Test
    fun sendBinary_emitsFinalBinaryFrame() = runBlocking {
        val raw = FakeRawSession()
        val s = KudosWebSocketSession(raw)
        val payload = byteArrayOf(0, 1, -1, 127, -128)

        s.sendBinary(payload)

        val frame = raw.sentFrames.receive()
        assertTrue(frame is Frame.Binary)
        assertTrue(frame.fin, "The wrapper always sends a final (non-fragmented) binary frame")
        assertContentEquals(payload, frame.data)
    }

    @Test
    fun close_emitsCloseFrameWithGivenReason() = runBlocking {
        val raw = FakeRawSession()
        val s = KudosWebSocketSession(raw)

        s.close(CloseReason(CloseReason.Codes.GOING_AWAY, "maintenance"))

        val frame = raw.sentFrames.receive()
        assertTrue(frame is Frame.Close)
        val reason = frame.readReason()
        assertEquals(CloseReason.Codes.GOING_AWAY, reason?.knownReason)
        assertEquals("maintenance", reason?.message)
    }

    @Test
    fun close_withoutArgument_usesNormalEmptyReason() = runBlocking {
        val raw = FakeRawSession()
        val s = KudosWebSocketSession(raw)

        s.close()

        val frame = raw.sentFrames.receive()
        assertTrue(frame is Frame.Close)
        val reason = frame.readReason()
        assertEquals(CloseReason.Codes.NORMAL, reason?.knownReason)
        assertEquals("", reason?.message)
    }

    /**
     * Minimal hand-rolled [DefaultWebSocketServerSession]: `outgoing` is a real buffered channel
     * ([sentFrames]) and the inherited default `send` member delivers into it; everything else is
     * inert. `call` deliberately throws — no test may touch the HTTP layer.
     */
    private class FakeRawSession : DefaultWebSocketServerSession {
        val sentFrames = Channel<Frame>(capacity = 16)

        override val call: ApplicationCall get() = throw UnsupportedOperationException("not used in unit tests")
        override val coroutineContext: CoroutineContext = EmptyCoroutineContext
        override val incoming: ReceiveChannel<Frame> = Channel()
        override val outgoing: SendChannel<Frame> get() = sentFrames
        override val extensions: List<WebSocketExtension<*>> = emptyList()
        override var masking: Boolean = false
        override var maxFrameSize: Long = Long.MAX_VALUE
        override var pingIntervalMillis: Long = -1
        override var timeoutMillis: Long = 15_000
        override val closeReason: Deferred<CloseReason?> = CompletableDeferred()
        @OptIn(InternalAPI::class)
        override fun start(negotiatedExtensions: List<WebSocketExtension<*>>) {}
        override suspend fun flush() {}
        override fun terminate() {}
    }
}
