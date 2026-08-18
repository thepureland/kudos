package io.kudos.ability.comm.websocket.common.handler

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for the [IKudosWebSocketHandler] default implementations: every hook
 * (onConnect / onText / onBinary / onDisconnect — with and without an explicit cause)
 * must be a no-op that neither throws nor touches the session.
 *
 * The session is a [KudosWebSocketSessionRef] stub that records any I/O attempt, rather than a real
 * engine-backed session. That is what lets this test live in the engine-neutral module at all — the
 * SPI is declared against the interface, so exercising it needs no engine on the classpath.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class IKudosWebSocketHandlerTest {

    /** Implementation overriding nothing — exercises every interface default body. */
    private val handler = object : IKudosWebSocketHandler {}

    @Test
    fun allDefaultHooks_areNoOpsAndNeverTouchTheSession(): Unit = runBlocking {
        val session = RecordingSession()

        handler.onConnect(session)
        handler.onText(session, "any text")
        handler.onText(session, "")
        handler.onBinary(session, byteArrayOf())
        handler.onBinary(session, byteArrayOf(0, -1, 127))
        handler.onDisconnect(session) // default cause = null
        handler.onDisconnect(session, IllegalStateException("abnormal close"))

        // Reaching this line means every default hook completed without throwing. The assertion adds
        // the other half of "no-op": a default body must not have written to or closed the connection.
        assertTrue(session.interactions.isEmpty(), "default hooks touched the session: ${session.interactions}")
    }

    /** Records every I/O call so the test can assert none happened. */
    private class RecordingSession : KudosWebSocketSessionRef {
        override val sessionId: String = "s1"
        override val userId: String? = null
        override val tenantId: String? = null
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()

        val interactions = mutableListOf<String>()

        override suspend fun sendText(text: String) { interactions += "sendText" }
        override suspend fun sendBinary(bytes: ByteArray) { interactions += "sendBinary" }
        override suspend fun close(reason: WebSocketCloseReason) { interactions += "close" }
    }
}
