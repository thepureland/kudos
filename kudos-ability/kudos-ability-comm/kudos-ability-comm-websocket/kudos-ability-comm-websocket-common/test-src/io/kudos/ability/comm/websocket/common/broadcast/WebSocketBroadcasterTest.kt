package io.kudos.ability.comm.websocket.common.broadcast

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketPayload
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Unit tests for [WebSocketBroadcaster] using stub [KudosWebSocketSessionRef]s — no Ktor engine.
 *
 * Covers:
 * - broadcast: empty registry short-circuit (returns 0), full fan-out count, partial-failure
 *   counting (a failing session contributes 0 without stopping siblings), Unicode payloads;
 * - broadcastToUser / broadcastToTenant: index filtering and unknown-id empty result;
 * - unicast: success (true), unknown sessionId (false), sendText throwing (false, logged);
 * - binary payloads take the sendBinary path;
 * - the send deadline: a stuck session is counted as a miss and (by default) closed;
 * - the in-flight cap bounds how many sends run at once.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketBroadcasterTest {

    @Test
    fun broadcast_emptyRegistry_returnsZero(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)

        assertEquals(0, broadcaster.broadcast("nobody-home"))
    }

    @Test
    fun broadcast_reachesAllSessions_andReturnsCount(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val s1 = RecordingSession("s1").also(registry::register)
        val s2 = RecordingSession("s2").also(registry::register)
        val s3 = RecordingSession("s3").also(registry::register)

        val count = broadcaster.broadcast("hello")

        assertEquals(3, count)
        assertEquals(listOf("hello"), s1.received)
        assertEquals(listOf("hello"), s2.received)
        assertEquals(listOf("hello"), s3.received)
    }

    @Test
    fun broadcast_failingSession_doesNotStopOthers_andIsNotCounted(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val ok1 = RecordingSession("ok1").also(registry::register)
        val bad = CrashingSession("bad").also(registry::register)
        val ok2 = RecordingSession("ok2").also(registry::register)

        val count = broadcaster.broadcast("msg")

        assertEquals(2, count, "Only successful sends are counted")
        assertEquals(listOf("msg"), ok1.received)
        assertEquals(listOf("msg"), ok2.received)
        assertEquals(1, bad.attempts, "The failing session is still attempted exactly once")
    }

    @Test
    fun broadcast_unicodeAndEmptyText_areDeliveredVerbatim(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val s = RecordingSession("s1").also(registry::register)

        assertEquals(1, broadcaster.broadcast(""))
        assertEquals(1, broadcaster.broadcast("中文🚀 "))

        assertEquals(listOf("", "中文🚀 "), s.received)
    }

    @Test
    fun broadcastToUser_onlyReachesThatUsersSessions(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val phone = RecordingSession("s1", userId = "u1").also(registry::register)
        val laptop = RecordingSession("s2", userId = "u1").also(registry::register)
        val other = RecordingSession("s3", userId = "u2").also(registry::register)

        val count = broadcaster.broadcastToUser("u1", "hi-u1")

        assertEquals(2, count)
        assertEquals(listOf("hi-u1"), phone.received)
        assertEquals(listOf("hi-u1"), laptop.received)
        assertTrue(other.received.isEmpty())
    }

    @Test
    fun broadcastToUser_unknownUser_returnsZero(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        registry.register(RecordingSession("s1", userId = "u1"))
        val broadcaster = WebSocketBroadcaster(registry)

        assertEquals(0, broadcaster.broadcastToUser("no-such-user", "x"))
    }

    @Test
    fun broadcastToTenant_onlyReachesThatTenantsSessions(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val t1a = RecordingSession("s1", tenantId = "t1").also(registry::register)
        val t1b = RecordingSession("s2", tenantId = "t1").also(registry::register)
        val t2 = RecordingSession("s3", tenantId = "t2").also(registry::register)

        val count = broadcaster.broadcastToTenant("t1", "tenant-msg")

        assertEquals(2, count)
        assertEquals(listOf("tenant-msg"), t1a.received)
        assertEquals(listOf("tenant-msg"), t1b.received)
        assertTrue(t2.received.isEmpty())
    }

    @Test
    fun broadcastToTenant_unknownTenant_returnsZero(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        registry.register(RecordingSession("s1", tenantId = "t1"))
        val broadcaster = WebSocketBroadcaster(registry)

        assertEquals(0, broadcaster.broadcastToTenant("no-such-tenant", "x"))
    }

    @Test
    fun unicast_existingSession_returnsTrueAndDelivers(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val s = RecordingSession("s1").also(registry::register)

        assertTrue(broadcaster.unicast("s1", "direct"))
        assertEquals(listOf("direct"), s.received)
    }

    @Test
    fun unicast_unknownSessionId_returnsFalse(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)

        assertFalse(broadcaster.unicast("ghost", "x"))
    }

    @Test
    fun unicast_sendFailure_returnsFalseWithoutThrowing(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val bad = CrashingSession("bad").also(registry::register)

        assertFalse(broadcaster.unicast("bad", "x"), "A throwing sendText must surface as false, not as an exception")
        assertEquals(1, bad.attempts)
    }

    @Test
    fun binaryPayloads_takeTheSendBinaryPath(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val s = RecordingSession("s1", userId = "u1", tenantId = "t1").also(registry::register)
        val payload = byteArrayOf(1, -2, 3)

        assertEquals(1, broadcaster.broadcast(payload))
        assertEquals(1, broadcaster.broadcastToUser("u1", payload))
        assertEquals(1, broadcaster.broadcastToTenant("t1", payload))
        assertTrue(broadcaster.unicast("s1", payload))

        assertTrue(s.received.isEmpty(), "Binary payloads must not be routed through sendText")
        assertEquals(4, s.receivedBinary.size)
        s.receivedBinary.forEach { assertContentEquals(payload, it) }
    }

    @Test
    fun payloadOverload_isEquivalentToTheTextOverload(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry)
        val s = RecordingSession("s1").also(registry::register)

        broadcaster.broadcast(WebSocketPayload.Text("via-payload"))

        assertEquals(listOf("via-payload"), s.received)
    }

    @Test
    fun sendTimeout_countsAsMiss_andClosesTheStuckSession(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry, sendTimeout = 50.milliseconds)
        val healthy = RecordingSession("ok").also(registry::register)
        val stuck = StuckSession("stuck").also(registry::register)

        val count = broadcaster.broadcast("msg")

        assertEquals(1, count, "A session that never completes its send is not a delivery")
        assertEquals(listOf("msg"), healthy.received, "A stuck sibling must not hold up the rest of the fan-out")
        assertTrue(stuck.closed, "The default policy closes a client that is not draining its socket")
    }

    @Test
    fun sendTimeout_withCloseDisabled_leavesTheSessionOpen(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry, sendTimeout = 50.milliseconds, closeOnSendTimeout = false)
        val stuck = StuckSession("stuck").also(registry::register)

        assertFalse(broadcaster.unicast("stuck", "msg"))
        assertFalse(stuck.closed, "closeOnSendTimeout = false must leave the connection alone")
    }

    @Test
    fun maxConcurrentSends_boundsInFlightSends(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val broadcaster = WebSocketBroadcaster(registry, maxConcurrentSends = 2)
        val inFlight = AtomicInteger()
        val peak = AtomicInteger()
        repeat(8) { i ->
            registry.register(SlowSession("s$i", inFlight, peak))
        }

        assertEquals(8, broadcaster.broadcast("msg"), "Every session is still delivered to, just not all at once")
        assertTrue(peak.get() <= 2, "At most 2 sends may be in flight, observed peak ${peak.get()}")
    }

    /** Captures `sendText` / `sendBinary` payloads in order; thread-safe under the concurrent broadcaster. */
    private class RecordingSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        val received: MutableList<String> = CopyOnWriteArrayList()
        val receivedBinary: MutableList<ByteArray> = CopyOnWriteArrayList()
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) { received += text }
        override suspend fun sendBinary(bytes: ByteArray) { receivedBinary += bytes }
        override suspend fun close(reason: WebSocketCloseReason) {}
    }

    /** Always throws on send — used to assert failure isolation and counting. */
    private class CrashingSession(override val sessionId: String) : KudosWebSocketSessionRef {
        override val userId: String? = null
        override val tenantId: String? = null
        var attempts: Int = 0
            private set
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {
            attempts++
            error("send blew up")
        }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }

    /** Never completes a send — models a client that has stopped reading its socket. */
    private class StuckSession(override val sessionId: String) : KudosWebSocketSessionRef {
        override val userId: String? = null
        override val tenantId: String? = null
        @Volatile var closed: Boolean = false
            private set
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) { delay(Long.MAX_VALUE) }
        override suspend fun sendBinary(bytes: ByteArray) { delay(Long.MAX_VALUE) }
        override suspend fun close(reason: WebSocketCloseReason) { closed = true }
    }

    /** Records how many sends overlap, so the concurrency cap can be asserted. */
    private class SlowSession(
        override val sessionId: String,
        private val inFlight: AtomicInteger,
        private val peak: AtomicInteger,
    ) : KudosWebSocketSessionRef {
        override val userId: String? = null
        override val tenantId: String? = null
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {
            val now = inFlight.incrementAndGet()
            peak.updateAndGet { maxOf(it, now) }
            try {
                delay(30)
            } finally {
                inFlight.decrementAndGet()
            }
        }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }
}
