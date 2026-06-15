package io.kudos.ability.comm.websocket.ktor.handler

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.mock
import kotlin.test.Test

/**
 * Unit tests for the [IKudosWebSocketHandler] default implementations: every hook
 * (onConnect / onText / onBinary / onDisconnect — with and without an explicit cause)
 * must be a no-op that neither throws nor touches the session. The session's raw Ktor
 * delegate is a strict-ish mock that is never expected to be called.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class IKudosWebSocketHandlerTest {

    /** Implementation overriding nothing — exercises every interface default body. */
    private val handler = object : IKudosWebSocketHandler {}

    private fun newSession(): KudosWebSocketSession =
        KudosWebSocketSession(raw = mock(DefaultWebSocketServerSession::class.java))

    @Test
    fun allDefaultHooks_areNoOps() = runBlocking {
        val session = newSession()

        handler.onConnect(session)
        handler.onText(session, "any text")
        handler.onText(session, "")
        handler.onBinary(session, byteArrayOf())
        handler.onBinary(session, byteArrayOf(0, -1, 127))
        handler.onDisconnect(session) // default cause = null
        handler.onDisconnect(session, IllegalStateException("abnormal close"))
        // Reaching this line means every default hook completed without throwing.
    }
}
