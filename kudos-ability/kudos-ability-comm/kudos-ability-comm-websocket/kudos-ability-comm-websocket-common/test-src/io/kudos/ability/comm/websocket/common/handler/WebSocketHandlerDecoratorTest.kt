package io.kudos.ability.comm.websocket.common.handler

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for the per-message interception seam: [WebSocketHandlerDecorator] and [wrappedBy].
 *
 * Covers:
 * - the base class delegates every hook untouched when nothing is overridden;
 * - `wrappedBy` composes outermost-first, and an empty chain returns the handler itself;
 * - a decorator can run code both before and after the delegate (the "around" capability that a
 *   preHandle/postHandle style interceptor could not offer) and can short-circuit it entirely.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketHandlerDecoratorTest {

    private val session: KudosWebSocketSessionRef = StubSession("s1")

    @Test
    fun baseClass_delegatesEveryHookUnchanged(): Unit = runBlocking {
        val terminal = RecordingHandler("t")
        val decorator = object : WebSocketHandlerDecorator(terminal) {}
        val cause = IllegalStateException("boom")

        decorator.onConnect(session)
        decorator.onText(session, "hi")
        decorator.onBinary(session, byteArrayOf(1, 2))
        decorator.onDisconnect(session, cause)

        assertEquals(listOf("t:connect", "t:text:hi", "t:binary:[1, 2]", "t:disconnect:boom"), terminal.events)
        assertContentEquals(listOf(session, session, session, session), terminal.sessions)
    }

    @Test
    fun wrappedBy_appliesFactoriesOutermostFirst(): Unit = runBlocking {
        val trace = mutableListOf<String>()
        val terminal = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) { trace += "terminal:$text" }
        }

        val chain = terminal.wrappedBy(
            { next -> TracingDecorator("outer", next, trace) },
            { next -> TracingDecorator("inner", next, trace) },
        )
        chain.onText(session, "msg")

        assertEquals(
            listOf("outer>", "inner>", "terminal:msg", "inner<", "outer<"),
            trace,
            "The first factory must end up outermost, so the chain reads in execution order",
        )
    }

    @Test
    fun wrappedBy_emptyChain_returnsTheHandlerItself() {
        val terminal = RecordingHandler("t")

        assertSame(terminal, terminal.wrappedBy())
        assertSame(terminal, terminal.wrappedBy(emptyList()))
    }

    @Test
    fun decorator_canShortCircuitTheDelegate(): Unit = runBlocking {
        val terminal = RecordingHandler("t")
        val chain = terminal.wrappedBy({ next -> BlockingDecorator(next) })

        chain.onText(session, "dropped")
        chain.onBinary(session, byteArrayOf(9))

        assertEquals(emptyList(), terminal.events, "A decorator that does not call through must stop the message")
    }

    /** Records enter/exit around the delegate — proves "around" advice, not just before/after hooks. */
    private class TracingDecorator(
        private val name: String,
        next: IKudosWebSocketHandler,
        private val trace: MutableList<String>,
    ) : WebSocketHandlerDecorator(next) {
        override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
            trace += "$name>"
            try {
                super.onText(session, text)
            } finally {
                trace += "$name<"
            }
        }
    }

    /** Never calls through. */
    private class BlockingDecorator(next: IKudosWebSocketHandler) : WebSocketHandlerDecorator(next) {
        override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {}
        override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {}
    }

    private class RecordingHandler(private val name: String) : IKudosWebSocketHandler {
        val events = mutableListOf<String>()
        val sessions = mutableListOf<KudosWebSocketSessionRef>()
        override suspend fun onConnect(session: KudosWebSocketSessionRef) {
            sessions += session; events += "$name:connect"
        }
        override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
            sessions += session; events += "$name:text:$text"
        }
        override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) {
            sessions += session; events += "$name:binary:${bytes.toList()}"
        }
        override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) {
            sessions += session; events += "$name:disconnect:${cause?.message}"
        }
    }

    private class StubSession(override val sessionId: String) : KudosWebSocketSessionRef {
        override val userId: String? = null
        override val tenantId: String? = null
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {}
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }
}
