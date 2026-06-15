package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit tests for the internal [dispatchFrames] loop, driven with a plain frame channel —
 * no Ktor engine. This is the only way to reach the Close / Ping / Pong branches: a real
 * `DefaultWebSocketSession` consumes those control frames before they ever appear on
 * `incoming`, so the e2e tests in [KudosWebSocketRoutingTest] cannot cover them.
 *
 * Covers:
 * - text / binary frames are dispatched to the matching handler hook, in arrival order,
 *   with verbatim (Unicode / empty) payloads and the same session instance;
 * - Ping / Pong frames fall into the `else` branch and are ignored;
 * - a Close frame breaks the loop immediately — later frames stay in the channel undispatched;
 * - a closed empty channel ends the loop without any handler invocation;
 * - handler exceptions are NOT swallowed here (the route's catch block owns that policy).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DispatchFramesTest {

    private fun newSession(): KudosWebSocketSession =
        KudosWebSocketSession(raw = mock(DefaultWebSocketServerSession::class.java), sessionId = "s-dispatch")

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
    fun handlerException_propagatesToCaller() = runBlocking {
        val frames = Channel<Frame>(capacity = 16)
        frames.send(Frame.Text("boom"))
        frames.close()
        val session = newSession()
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSession, text: String) =
                throw IllegalStateException("business handler blew up on: $text")
        }

        val e = assertFailsWith<IllegalStateException> { dispatchFrames(frames, session, handler) }
        assertEquals("business handler blew up on: boom", e.message)
    }

    /** Records hook invocations in order, together with the session instances seen. */
    private class RecordingHandler : IKudosWebSocketHandler {
        val events = mutableListOf<String>()
        val sessions = mutableListOf<KudosWebSocketSession>()
        override suspend fun onText(session: KudosWebSocketSession, text: String) {
            sessions += session
            events += "text:$text"
        }
        override suspend fun onBinary(session: KudosWebSocketSession, bytes: ByteArray) {
            sessions += session
            events += "binary:${bytes.toList()}"
        }
    }
}
