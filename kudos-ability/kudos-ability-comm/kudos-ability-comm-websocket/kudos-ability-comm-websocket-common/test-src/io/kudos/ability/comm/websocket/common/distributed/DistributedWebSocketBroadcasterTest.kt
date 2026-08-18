package io.kudos.ability.comm.websocket.common.distributed

import io.kudos.ability.comm.websocket.common.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.common.distributed.WebSocketBroadcastEnvelope.TargetType
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end tests for [DistributedWebSocketBroadcaster] using an [InMemoryBroadcastChannel] shared by
 * two simulated nodes. Each "node" has its own registry + local broadcaster + distributed decorator;
 * sessions live on one side or the other. The tests assert that:
 *  - cross-node delivery reaches sessions on the *other* node (the Redis-bridged behavior the kudos
 *    README marks as "leave to business");
 *  - self-echo filtering does not double-deliver on the originating node;
 *  - exceptions from `local` during inbound dispatch do not terminate the channel subscription;
 *  - publish failures are swallowed (local delivery survives a dead channel);
 *  - inbound envelopes with a null targetId for USER / TENANT / SESSION are dropped silently;
 *  - a local broadcaster that throws during inbound dispatch is caught (the listener stays alive);
 *  - unicast is local-first: a local hit costs the cluster nothing;
 *  - envelopes from a future schema version are dropped rather than half-interpreted;
 *  - closing the broadcaster detaches its subscription.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DistributedWebSocketBroadcasterTest {

    private fun newCluster(channel: InMemoryBroadcastChannel = InMemoryBroadcastChannel()): Cluster {
        val regA = KudosWebSocketRegistry()
        val regB = KudosWebSocketRegistry()
        val distA = DistributedWebSocketBroadcaster(WebSocketBroadcaster(regA), channel, nodeId = "node-A")
        val distB = DistributedWebSocketBroadcaster(WebSocketBroadcaster(regB), channel, nodeId = "node-B")
        return Cluster(regA, regB, distA, distB, channel)
    }

    @Test
    fun broadcast_reachesSessionsOnRemoteNode(): Unit = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A", tenantId = "t-1").also(cluster.regA::register)
        val sB = RecordingSession("s-B", userId = "u-B", tenantId = "t-1").also(cluster.regB::register)

        val locallyReached = cluster.distA.broadcast("hello-all")

        assertEquals(1, locallyReached, "broadcast() returns the count of *local* sessions reached")
        assertEquals(listOf("hello-all"), sA.received, "Local session on A receives the message directly")
        assertEquals(listOf("hello-all"), sB.received, "Remote session on B receives the message via the channel")
    }

    @Test
    fun broadcastToUser_isRoutedAcrossNodes(): Unit = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A").also(cluster.regA::register)
        val sB = RecordingSession("s-B", userId = "u-A").also(cluster.regB::register)
        val sBother = RecordingSession("s-B-other", userId = "u-other").also(cluster.regB::register)

        cluster.distA.broadcastToUser("u-A", "hi-u-A")

        assertEquals(listOf("hi-u-A"), sA.received)
        assertEquals(listOf("hi-u-A"), sB.received)
        assertTrue(sBother.received.isEmpty(), "Sessions for a different userId on the remote node are not reached")
    }

    @Test
    fun broadcastToTenant_isRoutedAcrossNodes(): Unit = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", tenantId = "t-1").also(cluster.regA::register)
        val sB = RecordingSession("s-B", tenantId = "t-1").also(cluster.regB::register)
        val sBother = RecordingSession("s-B-other", tenantId = "t-2").also(cluster.regB::register)

        cluster.distA.broadcastToTenant("t-1", "tenant-1-only")

        assertEquals(listOf("tenant-1-only"), sA.received)
        assertEquals(listOf("tenant-1-only"), sB.received)
        assertTrue(sBother.received.isEmpty(), "Sessions for a different tenant on the remote node are not reached")
    }

    @Test
    fun binaryPayload_isRoutedAcrossNodes(): Unit = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A").also(cluster.regA::register)
        val sB = RecordingSession("s-B", userId = "u-A").also(cluster.regB::register)
        val payload = byteArrayOf(9, -8, 7)

        cluster.distA.broadcastToUser("u-A", payload)

        assertContentEquals(payload, sA.receivedBinary.single(), "Local binary delivery")
        assertContentEquals(payload, sB.receivedBinary.single(), "Binary must survive the envelope round-trip")
    }

    @Test
    fun unicast_remoteSession_returnsLocalFalseButRemoteDelivers(): Unit = runBlocking {
        val cluster = newCluster()
        val sB = RecordingSession("s-B").also(cluster.regB::register)

        // A unicasts to a sessionId that is registered on B, not A.
        val localOk = cluster.distA.unicast("s-B", "ping")

        assertFalse(localOk, "A's local registry does not hold s-B → unicast returns local-false")
        assertEquals(listOf("ping"), sB.received, "B still receives the message via channel-backed delivery")
    }

    @Test
    fun unicast_localHit_doesNotPublishToTheCluster(): Unit = runBlocking {
        val channel = CapturingChannel()
        val registry = KudosWebSocketRegistry()
        val session = RecordingSession("s-1").also(registry::register)
        val dist = DistributedWebSocketBroadcaster(WebSocketBroadcaster(registry), channel, nodeId = "node-local")

        assertTrue(dist.unicast("s-1", "local-only"))

        assertEquals(listOf("local-only"), session.received)
        assertTrue(
            channel.published.isEmpty(),
            "A sessionId is unique cluster-wide, so a local hit must not make every other node deserialize an envelope it cannot use",
        )
    }

    @Test
    fun selfEcho_doesNotDoubleDeliverOnOriginatingNode(): Unit = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A").also(cluster.regA::register)

        cluster.distA.broadcastToUser("u-A", "once")

        assertEquals(
            listOf("once"), sA.received,
            "Originating-node session must receive exactly one delivery — local fan-out only, " +
                "the pub/sub self-delivery is dropped by the nodeId filter.",
        )
    }

    @Test
    fun inboundDispatch_failureDoesNotTerminateSubscription(): Unit = runBlocking {
        val channel = InMemoryBroadcastChannel()
        val cluster = newCluster(channel)
        val sB = RecordingSession("s-B", userId = "u-A").also(cluster.regB::register)
        val sBcrashing = CrashingSession("s-B-crash", userId = "u-A").also(cluster.regB::register)

        // First inbound delivery causes one session to throw; subsequent calls must still work.
        cluster.distA.broadcastToUser("u-A", "first")
        cluster.distA.broadcastToUser("u-A", "second")

        assertEquals(listOf("first", "second"), sB.received,
            "Subsequent broadcasts must still reach the healthy remote session after a sibling session threw.")
        assertEquals(2, sBcrashing.attempts, "The crashing session is still attempted on every delivery (not blacklisted).")
    }

    @Test
    fun unicast_localSession_returnsTrue(): Unit = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A").also(cluster.regA::register)

        assertTrue(cluster.distA.unicast("s-A", "to-self"), "Local registry holds s-A → unicast returns true")
        assertEquals(listOf("to-self"), sA.received)
    }

    @Test
    fun publishFailure_isSwallowed_andLocalDeliveryStillHappens(): Unit = runBlocking {
        val registry = KudosWebSocketRegistry()
        val session = RecordingSession("s-1", userId = "u-1", tenantId = "t-1").also(registry::register)
        val dist = DistributedWebSocketBroadcaster(WebSocketBroadcaster(registry), CrashingChannel(), nodeId = "node-X")

        // Every broadcast variant must survive a channel whose publish always throws.
        assertEquals(1, dist.broadcast("a"))
        assertEquals(1, dist.broadcastToUser("u-1", "b"))
        assertEquals(1, dist.broadcastToTenant("t-1", "c"))
        assertTrue(dist.unicast("s-1", "d"))

        assertEquals(listOf("a", "b", "c", "d"), session.received,
            "Local sessions keep receiving even when the distributed channel is down")
    }

    @Test
    fun inbound_nullTargetId_forTargetedTypes_isDroppedSilently(): Unit = runBlocking {
        val channel = CapturingChannel()
        val registry = KudosWebSocketRegistry()
        val session = RecordingSession("s-1", userId = "u-1", tenantId = "t-1").also(registry::register)
        DistributedWebSocketBroadcaster(WebSocketBroadcaster(registry), channel, nodeId = "node-local")
        val handler = channel.handler ?: error("broadcaster must subscribe on construction")

        // Foreign-node envelopes whose targetId is null: USER / TENANT / SESSION must all be no-ops.
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.USER, targetId = null, text = "x"))
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.TENANT, targetId = null, text = "x"))
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.SESSION, targetId = null, text = "x"))
        assertTrue(session.received.isEmpty(), "null targetId must not fan out to anyone")

        // Sanity check the same wiring delivers when targetId is present.
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.USER, targetId = "u-1", text = "y"))
        assertEquals(listOf("y"), session.received)
    }

    @Test
    fun inbound_envelopeFromANewerSchemaVersion_isDropped(): Unit = runBlocking {
        val channel = CapturingChannel()
        val registry = KudosWebSocketRegistry()
        val session = RecordingSession("s-1", userId = "u-1").also(registry::register)
        DistributedWebSocketBroadcaster(WebSocketBroadcaster(registry), channel, nodeId = "node-local")
        val handler = channel.handler ?: error("broadcaster must subscribe on construction")

        handler(
            WebSocketBroadcastEnvelope(
                "node-remote", TargetType.USER, "u-1", text = "from-the-future",
                version = WebSocketBroadcastEnvelope.CURRENT_VERSION + 1,
            )
        )
        assertTrue(session.received.isEmpty(), "A half-understood envelope is worse than a dropped one during a rolling upgrade")

        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.USER, "u-1", text = "current"))
        assertEquals(listOf("current"), session.received, "The current version still delivers")
    }

    @Test
    fun inbound_localBroadcasterThrowing_isCaught_andListenerSurvives(): Unit = runBlocking {
        val channel = CapturingChannel()
        // Final-class mock whose every method throws — simulates a local broadcaster blowing up mid-dispatch.
        val throwingLocal = Mockito.mock(WebSocketBroadcaster::class.java) {
            throw IllegalStateException("local fan-out failed")
        }
        DistributedWebSocketBroadcaster(throwingLocal, channel, nodeId = "node-local")
        val handler = channel.handler ?: error("broadcaster must subscribe on construction")

        // None of the four inbound target types may propagate the local broadcaster's exception.
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.ALL, targetId = null, text = "x"))
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.USER, targetId = "u", text = "x"))
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.TENANT, targetId = "t", text = "x"))
        handler(WebSocketBroadcastEnvelope("node-remote", TargetType.SESSION, targetId = "s", text = "x"))
        // Reaching this line means every exception was contained inside onInbound.
    }

    @Test
    fun close_detachesTheSubscription_andStopsInboundDelivery(): Unit = runBlocking {
        val channel = InMemoryBroadcastChannel()
        val cluster = newCluster(channel)
        val sB = RecordingSession("s-B", userId = "u-A").also(cluster.regB::register)
        assertEquals(2, channel.subscriberCount, "Both nodes subscribe on construction")

        cluster.distB.close()

        assertEquals(1, channel.subscriberCount, "close() must detach the handler, not leak it for the process lifetime")
        cluster.distA.broadcastToUser("u-A", "after-close")
        assertTrue(sB.received.isEmpty(), "A closed broadcaster no longer delivers inbound traffic")
    }

    @Test
    fun envelope_carriesNodeIdTargetAndText(): Unit = runBlocking {
        val channel = CapturingChannel()
        val dist = DistributedWebSocketBroadcaster(WebSocketBroadcaster(KudosWebSocketRegistry()), channel, nodeId = "node-42")

        dist.broadcast("all-text")
        dist.broadcastToUser("u-9", "user-text")
        dist.broadcastToTenant("t-9", "tenant-text")
        dist.unicast("s-9", "session-text")

        assertEquals(
            listOf(
                WebSocketBroadcastEnvelope("node-42", TargetType.ALL, null, "all-text"),
                WebSocketBroadcastEnvelope("node-42", TargetType.USER, "u-9", "user-text"),
                WebSocketBroadcastEnvelope("node-42", TargetType.TENANT, "t-9", "tenant-text"),
                WebSocketBroadcastEnvelope("node-42", TargetType.SESSION, "s-9", "session-text"),
            ),
            channel.published,
        )
    }

    /** Records the subscribed handler and every published envelope without delivering anything. */
    private class CapturingChannel : IWebSocketBroadcastChannel {
        var handler: (suspend (WebSocketBroadcastEnvelope) -> Unit)? = null
        val published: MutableList<WebSocketBroadcastEnvelope> = CopyOnWriteArrayList()
        override suspend fun publish(envelope: WebSocketBroadcastEnvelope) { published += envelope }
        override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit): WebSocketBroadcastSubscription {
            this.handler = handler
            return WebSocketBroadcastSubscription { this.handler = null }
        }
    }

    /** publish always throws; subscribe is accepted — models a down transport. */
    private class CrashingChannel : IWebSocketBroadcastChannel {
        override suspend fun publish(envelope: WebSocketBroadcastEnvelope): Unit = error("transport down")
        override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit) =
            WebSocketBroadcastSubscription { }
    }

    private data class Cluster(
        val regA: KudosWebSocketRegistry,
        val regB: KudosWebSocketRegistry,
        val distA: DistributedWebSocketBroadcaster,
        val distB: DistributedWebSocketBroadcaster,
        val channel: InMemoryBroadcastChannel,
    )

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

    /** Always throws on send — used to assert one bad session does not poison the channel subscription. */
    private class CrashingSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        var attempts: Int = 0
            private set
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {
            attempts++
            error("boom")
        }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }
}
