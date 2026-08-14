package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit tests for the internal [dispatchFrames] loop, driven with a plain frame channel —
 * no Ktor engine. This is the only way to reach the Close / Ping / Pong and fragmentation
 * branches: a real `DefaultWebSocketSession` consumes control frames before they appear on
 * `incoming`, and fragmentation is awkward to provoke through a client.
 *
 * Covers:
 * - text / binary frames are dispatched to the matching handler hook, in arrival order,
 *   with verbatim (Unicode / empty) payloads and the same session instance;
 * - fragmented messages are reassembled into a single hook call, including a multi-byte UTF-8
 *   character split across the fragment boundary;
 * - Ping / Pong frames are ignored, even when they interleave a fragmented message;
 * - a Close frame breaks the loop immediately — later frames stay in the channel undispatched;
 * - a closed empty channel ends the loop without any handler invocation;
 * - an over-sized message closes the connection instead of buffering without bound;
 * - handler exceptions are NOT swallowed here (the route's catch block owns that policy).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DispatchFramesTest {

    private fun newSession() = StubSession("s-dispatch")

    @Test
    fun textAndBinaryFrames_areDispatchedInOrderWithSameSession() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val session = newSession()
        val handler = RecordingHandler()
        frames.send(Frame.Text("héllo 中文🚀"))
        frames.send(Frame.Text(""))
        frames.send(Frame.Binary(true, byteArrayOf(1, -2, 3)))
        frames.close()

        dispatchFrames(frames, session, handler)

        assertEquals(listOf("text:héllo 中文🚀", "text:", "binary:[1, -2, 3]"), handler.events)
        assertTrue(handler.sessions.all { it === session }, "Every hook must receive the same wrapped session")
    }

    @Test
    fun fragmentedTextMessage_isReassembledIntoOneCall() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        // Ktor labels continuation frames with the original opcode and only flags the last one `fin`.
        frames.send(Frame.Text(false, "Hello, ".toByteArray()))
        frames.send(Frame.Text(false, "fragmented ".toByteArray()))
        frames.send(Frame.Text(true, "world".toByteArray()))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("text:Hello, fragmented world"), handler.events,
            "The handler must see one whole message, not three partial ones")
    }

    @Test
    fun fragmentBoundarySplittingAMultiByteChar_stillDecodesCorrectly() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        val message = "中文🚀"
        val bytes = message.toByteArray()
        // Split in the middle of the first character's UTF-8 sequence: decoding each fragment on its
        // own would yield replacement characters rather than the original text.
        frames.send(Frame.Text(false, bytes.copyOfRange(0, 2)))
        frames.send(Frame.Text(true, bytes.copyOfRange(2, bytes.size)))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("text:$message"), handler.events)
    }

    @Test
    fun fragmentedBinaryMessage_isReassembled() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        frames.send(Frame.Binary(false, byteArrayOf(1, 2)))
        frames.send(Frame.Binary(true, byteArrayOf(3, 4)))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("binary:[1, 2, 3, 4]"), handler.events)
    }

    @Test
    fun controlFramesInterleavingAFragmentedMessage_doNotCorruptIt() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        frames.send(Frame.Text(false, "part-1 ".toByteArray()))
        // Legal per RFC 6455: control frames may be injected between fragments.
        frames.send(Frame.Ping(byteArrayOf(1)))
        frames.send(Frame.Pong(byteArrayOf(2)))
        frames.send(Frame.Text(true, "part-2".toByteArray()))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("text:part-1 part-2"), handler.events)
    }

    @Test
    fun consecutiveFragmentedMessages_areDispatchedSeparately() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        frames.send(Frame.Text(false, "a".toByteArray()))
        frames.send(Frame.Text(true, "b".toByteArray()))
        frames.send(Frame.Text(false, "c".toByteArray()))
        frames.send(Frame.Text(true, "d".toByteArray()))
        frames.send(Frame.Text("e"))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("text:ab", "text:cd", "text:e"), handler.events,
            "The reassembly buffer must reset between messages")
    }

    @Test
    fun pingAndPongFrames_areIgnored() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        frames.send(Frame.Ping(byteArrayOf(1)))
        frames.send(Frame.Pong(byteArrayOf(2)))
        frames.send(Frame.Text("after-control"))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("text:after-control"), handler.events,
            "Ping / Pong must be silently skipped, not dispatched and not loop-breaking")
    }

    @Test
    fun closeFrame_breaksLoop_leavingLaterFramesUnconsumed() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val handler = RecordingHandler()
        frames.send(Frame.Text("before-close"))
        frames.send(Frame.Close(CloseReason(CloseReason.Codes.NORMAL, "bye")))
        frames.send(Frame.Text("after-close"))
        frames.close()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(listOf("text:before-close"), handler.events, "Nothing after the Close frame may be dispatched")
        val leftover = frames.tryReceive().getOrNull()
        assertTrue(leftover is Frame.Text, "The loop must break on Close, leaving later frames in the channel")
        assertEquals("after-close", leftover.readText())
    }

    @Test
    fun closedEmptyChannel_endsLoopWithoutHandlerCalls() = runBlocking {
        val frames = Channel<Frame>(capacity = 1)
        frames.close()
        val handler = RecordingHandler()

        dispatchFrames(frames, newSession(), handler)

        assertEquals(emptyList(), handler.events)
        assertNull(handler.sessions.firstOrNull())
    }

    @Test
    fun singleFrameOverTheSizeLimit_closesTheConnectionWithoutDispatching() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val session = newSession()
        val handler = RecordingHandler()
        frames.send(Frame.Text(true, ByteArray(64)))
        frames.close()

        dispatchFrames(frames, session, handler, maxMessageSize = 16)

        assertEquals(emptyList(), handler.events, "An over-sized message must not reach the handler")
        assertEquals(CloseReason.Codes.TOO_BIG.code, session.closeReason?.code)
    }

    @Test
    fun fragmentsExceedingTheSizeLimit_closeTheConnectionInsteadOfBuffering() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        val session = newSession()
        val handler = RecordingHandler()
        // Each fragment is under the cap; their sum is not. Without the running check, the buffer
        // grows unbounded because kudos-ability-web-ktor leaves maxFrameSize at Long.MAX_VALUE.
        frames.send(Frame.Text(false, ByteArray(10)))
        frames.send(Frame.Text(false, ByteArray(10)))
        frames.send(Frame.Text(true, ByteArray(10)))
        frames.close()

        dispatchFrames(frames, session, handler, maxMessageSize = 16)

        assertEquals(emptyList(), handler.events)
        assertEquals(CloseReason.Codes.TOO_BIG.code, session.closeReason?.code)
    }

    @Test
    fun handlerException_propagatesToCaller() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        frames.send(Frame.Text("boom"))
        frames.close()
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) =
                throw IllegalStateException("business handler blew up on: $text")
        }

        val e = assertFailsWith<IllegalStateException> { dispatchFrames(frames, newSession(), handler) }
        assertEquals("business handler blew up on: boom", e.message)
    }

    /** Records hook invocations in order, together with the session instances seen. */
    private class RecordingHandler : IKudosWebSocketHandler {
        val events = mutableListOf<String>()
        val sessions = mutableListOf<KudosWebSocketSessionRef>()
        override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
            sessions += session
            events += "text:$text"
        }
        override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {
            sessions += session
            events += "binary:${bytes.toList()}"
        }
    }

    /**
     * Plain session ref — no Ktor mock needed now that the handler SPI is expressed in terms of
     * [KudosWebSocketSessionRef]. Records the close reason so the size-limit policy can be asserted.
     */
    private class StubSession(override val sessionId: String) : KudosWebSocketSessionRef {
        override val userId: String? = null
        override val tenantId: String? = null
        var closeReason: CloseReason? = null
            private set
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {}
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: CloseReason) { closeReason = reason }
    }
}
