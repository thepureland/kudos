package io.kudos.ability.comm.websocket.ktor.handler

import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [RateLimitingWebSocketHandler], driven by a fake clock so nothing sleeps.
 *
 * Covers: burst allowance, rejection past the burst, refill over time, per-session isolation
 * (the bucket lives in session attributes), binary messages sharing the same budget, the
 * [RateLimitingWebSocketHandler.onLimitExceeded] override point, and constructor validation.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RateLimitingWebSocketHandlerTest {

    private val clock = AtomicLong(0)
    private fun advanceSeconds(seconds: Double) {
        clock.addAndGet((seconds * 1_000_000_000L).toLong())
    }

    private fun limiter(
        delegate: IKudosWebSocketHandler,
        permitsPerSecond: Double = 10.0,
        burst: Int = 3,
    ) = RateLimitingWebSocketHandler(delegate, permitsPerSecond, burst, clock::get)

    @Test
    fun burst_isAllowed_thenFurtherMessagesAreDropped() = runBlocking {
        val delegate = RecordingHandler()
        val handler = limiter(delegate, burst = 3)
        val session = StubSession("s1")

        repeat(5) { handler.onText(session, "m$it") }

        assertEquals(listOf("m0", "m1", "m2"), delegate.texts,
            "The bucket starts full at `burst`; everything past it is dropped without reaching the delegate")
    }

    @Test
    fun tokensRefillOverTime() = runBlocking {
        val delegate = RecordingHandler()
        val handler = limiter(delegate, permitsPerSecond = 10.0, burst = 2)
        val session = StubSession("s1")

        repeat(3) { handler.onText(session, "first-$it") }
        assertEquals(2, delegate.texts.size)

        advanceSeconds(0.2) // 10/s → 2 tokens back
        handler.onText(session, "later-0")
        handler.onText(session, "later-1")
        handler.onText(session, "later-2")

        assertEquals(listOf("first-0", "first-1", "later-0", "later-1"), delegate.texts)
    }

    @Test
    fun refillIsCappedAtBurst() = runBlocking {
        val delegate = RecordingHandler()
        val handler = limiter(delegate, permitsPerSecond = 10.0, burst = 2)
        val session = StubSession("s1")

        advanceSeconds(60.0) // idle for a minute: 600 tokens' worth of time, but capacity is 2

        repeat(5) { handler.onText(session, "m$it") }

        assertEquals(listOf("m0", "m1"), delegate.texts, "An idle session must not bank unlimited credit")
    }

    @Test
    fun eachSessionGetsItsOwnBudget() = runBlocking {
        val delegate = RecordingHandler()
        val handler = limiter(delegate, burst = 2)
        val busy = StubSession("busy")
        val quiet = StubSession("quiet")

        repeat(4) { handler.onText(busy, "busy-$it") }
        handler.onText(quiet, "quiet-0")

        assertEquals(listOf("busy-0", "busy-1", "quiet-0"), delegate.texts,
            "One noisy connection must not consume another connection's allowance")
        assertTrue(quiet.attributes.containsKey(RateLimitingWebSocketHandler.ATTRIBUTE_KEY),
            "The bucket is kept in session attributes so it dies with the connection")
    }

    @Test
    fun textAndBinaryShareTheSameBudget() = runBlocking {
        val delegate = RecordingHandler()
        val handler = limiter(delegate, burst = 2)
        val session = StubSession("s1")

        handler.onText(session, "t")
        handler.onBinary(session, byteArrayOf(1))
        handler.onText(session, "dropped")
        handler.onBinary(session, byteArrayOf(2))

        assertEquals(listOf("t"), delegate.texts)
        assertEquals(1, delegate.binaries.size, "Binary messages draw on the same per-session bucket")
    }

    @Test
    fun onLimitExceeded_canBeOverriddenToCloseTheSession() = runBlocking {
        val delegate = RecordingHandler()
        val session = StubSession("s1")
        val handler = object : RateLimitingWebSocketHandler(delegate, 10.0, 1, clock::get) {
            override suspend fun onLimitExceeded(session: KudosWebSocketSessionRef) {
                session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "too chatty"))
            }
        }

        handler.onText(session, "ok")
        handler.onText(session, "over")

        assertEquals(listOf("ok"), delegate.texts)
        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, session.closeReason?.code,
            "Overriding the hook must be enough to change the policy from drop to close")
    }

    @Test
    fun invalidConfiguration_isRejectedAtConstruction() {
        val delegate = RecordingHandler()
        assertFailsWith<IllegalArgumentException> { RateLimitingWebSocketHandler(delegate, permitsPerSecond = 0.0) }
        assertFailsWith<IllegalArgumentException> { RateLimitingWebSocketHandler(delegate, burst = 0) }
    }

    private class RecordingHandler : IKudosWebSocketHandler {
        val texts = mutableListOf<String>()
        val binaries = mutableListOf<ByteArray>()
        override suspend fun onText(session: KudosWebSocketSessionRef, text: String) { texts += text }
        override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) { binaries += bytes }
    }

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
