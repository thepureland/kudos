package io.kudos.ability.comm.websocket.common.session

import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [WebSocketPayload].
 *
 * [WebSocketPayload.Binary] is deliberately **not** a data class: the generated `equals` / `hashCode`
 * would compare [WebSocketPayload.Binary.bytes] by reference, so two identical payloads would be
 * unequal. That silently breaks any assertion or de-duplication built on payload equality — including
 * the envelope comparisons in `DistributedWebSocketBroadcasterTest`. The hand-written implementations
 * are the reason those work, and nothing else pins them down.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketPayloadTest {

    @Test
    fun binaryEquality_isStructuralNotByReference() {
        val a = WebSocketPayload.Binary(byteArrayOf(1, 2, 3))
        val b = WebSocketPayload.Binary(byteArrayOf(1, 2, 3))

        assertFalse(a.bytes === b.bytes, "The point of the test is two distinct arrays")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun binaryEquality_distinguishesDifferentContent() {
        assertNotEquals(
            WebSocketPayload.Binary(byteArrayOf(1, 2, 3)),
            WebSocketPayload.Binary(byteArrayOf(1, 2, 4)),
        )
        assertNotEquals(
            WebSocketPayload.Binary(byteArrayOf(1, 2)),
            WebSocketPayload.Binary(byteArrayOf(1, 2, 0)),
        )
    }

    @Test
    fun binaryEquality_handlesEmptyAndSelf() {
        val empty = WebSocketPayload.Binary(byteArrayOf())

        assertEquals(empty, empty)
        assertEquals(WebSocketPayload.Binary(byteArrayOf()), empty)
        assertNotEquals<WebSocketPayload>(empty, WebSocketPayload.Text(""))
    }

    /** Payloads can be megabytes, so the representation prints the size rather than the content. */
    @Test
    fun binaryToString_printsTheSizeNotTheContent() {
        val text = WebSocketPayload.Binary(ByteArray(4096)).toString()

        assertEquals("Binary(4096 bytes)", text)
    }

    @Test
    fun textIsAValueType() {
        assertEquals(WebSocketPayload.Text("hi"), WebSocketPayload.Text("hi"))
        assertEquals(WebSocketPayload.Text("hi").hashCode(), WebSocketPayload.Text("hi").hashCode())
        assertNotEquals(WebSocketPayload.Text("hi"), WebSocketPayload.Text("ho"))
    }

    /**
     * The sealed hierarchy exists so fan-out components stay payload-agnostic instead of declaring a
     * parallel `xxxBytes` method per targeting mode; [KudosWebSocketSessionRef.send] is where that pays
     * off, so the dispatch is asserted rather than assumed.
     */
    @Test
    fun send_dispatchesToTheMatchingSessionMethod(): Unit = runBlocking {
        val session = RecordingSession()

        session.send(WebSocketPayload.Text("hello"))
        session.send(WebSocketPayload.Binary(byteArrayOf(7, 8)))

        assertEquals(listOf("text:hello", "binary:2"), session.calls)
    }

    @Test
    fun whenExhaustive_theHierarchyHasExactlyTwoCases() {
        val kinds = WebSocketPayload::class.sealedSubclasses.map { it.simpleName }.toSet()

        assertTrue(kinds == setOf("Text", "Binary"),
            "A new payload kind must be routed in send() and in WebSocketBroadcastEnvelope; got $kinds")
    }

    private class RecordingSession : KudosWebSocketSessionRef {
        override val sessionId: String = "s1"
        override val userId: String? = null
        override val tenantId: String? = null
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()

        val calls = mutableListOf<String>()

        override suspend fun sendText(text: String) { calls += "text:$text" }
        override suspend fun sendBinary(bytes: ByteArray) { calls += "binary:${bytes.size}" }
        override suspend fun close(reason: WebSocketCloseReason) = Unit
    }
}
