package io.kudos.ability.comm.websocket.spring.session

import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import io.kudos.ability.comm.websocket.common.session.WebSocketPayload
import io.kudos.ability.comm.websocket.spring.FakeWebSocketSession
import kotlinx.coroutines.runBlocking
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.TextMessage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SpringWebSocketSession] — the mapping from the engine-neutral session contract onto
 * Spring's [org.springframework.web.socket.WebSocketSession].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SpringWebSocketSessionTest {

    @Test
    fun sendText_producesATextMessage(): Unit = runBlocking {
        val raw = FakeWebSocketSession()
        val session = SpringWebSocketSession(raw)

        session.sendText("hello")

        assertIs<TextMessage>(raw.sent.single())
        assertEquals(listOf("hello"), raw.sentText)
    }

    @Test
    fun sendBinary_producesABinaryMessageWithTheSameBytes(): Unit = runBlocking {
        val raw = FakeWebSocketSession()
        val session = SpringWebSocketSession(raw)
        val bytes = byteArrayOf(0, 1, -1, 127, -128)

        session.sendBinary(bytes)

        val message = assertIs<BinaryMessage>(raw.sent.single())
        val received = ByteArray(message.payload.remaining()).also { message.payload.duplicate().get(it) }
        assertContentEquals(bytes, received)
    }

    @Test
    fun send_dispatchesByPayloadKind(): Unit = runBlocking {
        val raw = FakeWebSocketSession()
        val session = SpringWebSocketSession(raw)

        session.send(WebSocketPayload.Text("t"))
        session.send(WebSocketPayload.Binary(byteArrayOf(9)))

        assertIs<TextMessage>(raw.sent[0])
        assertIs<BinaryMessage>(raw.sent[1])
    }

    @Test
    fun close_mapsCodeAndReasonOntoCloseStatus(): Unit = runBlocking {
        val raw = FakeWebSocketSession()
        val session = SpringWebSocketSession(raw)

        session.close(WebSocketCloseReason(WebSocketCloseReason.Codes.GOING_AWAY, "maintenance"))

        assertEquals(1001, raw.closedWith?.code)
        assertEquals("maintenance", raw.closedWith?.reason)
    }

    @Test
    fun close_defaultsToNormal(): Unit = runBlocking {
        val raw = FakeWebSocketSession()

        SpringWebSocketSession(raw).close()

        assertEquals(1000, raw.closedWith?.code)
    }

    /**
     * The container fires `afterConnectionClosed` before the pump's own teardown runs, so the normal
     * shutdown path would otherwise always attempt a redundant close — and log an exception for it on
     * every single disconnect.
     */
    @Test
    fun close_onAnAlreadyClosedSession_isSkipped(): Unit = runBlocking {
        val raw = FakeWebSocketSession()
        raw.close(org.springframework.web.socket.CloseStatus.NO_CLOSE_FRAME)
        val session = SpringWebSocketSession(raw)

        session.close(WebSocketCloseReason.NORMAL)

        assertEquals(1006, raw.closedWith?.code, "The second close must not overwrite how the connection actually ended")
    }

    @Test
    fun sessionId_defaultsToTheContainerId() {
        val session = SpringWebSocketSession(FakeWebSocketSession(sessionId = "container-42"))

        assertEquals("container-42", session.sessionId,
            "Reusing the container's id is what makes a kudos log line correlate with the container's own")
    }

    @Test
    fun identityFields_defaultToAnonymous() {
        val session = SpringWebSocketSession(FakeWebSocketSession())

        assertNull(session.userId)
        assertNull(session.tenantId)
        assertTrue(session.attributes.isEmpty())
    }
}
