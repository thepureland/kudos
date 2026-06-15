package io.kudos.ability.comm.websocket.ktor.distributed

import io.kudos.ability.comm.websocket.ktor.broadcast.WebSocketBroadcaster
import io.kudos.ability.comm.websocket.ktor.distributed.WebSocketBroadcastEnvelope.TargetType
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSessionRef
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
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
 *  - a local broadcaster that throws during inbound dispatch is caught (the listener stays alive).
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
    fun broadcast_reachesSessionsOnRemoteNode() = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A", userId = "u-A", tenantId = "t-1").also(cluster.regA::register)
        val sB = RecordingSession("s-B", userId = "u-B", tenantId = "t-1").also(cluster.regB::register)

        val locallyReached = cluster.distA.broadcast("hello-all")

        assertEquals(1, locallyReached, "broadcast() returns the count of *local* sessions reached")
        assertEquals(listOf("hello-all"), sA.received, "Local session on A receives the message directly")
        assertEquals(listOf("hello-all"), sB.received, "Remote session on B receives the message via the channel")
    }

    @Test
    fun broadcastToUser_isRoutedAcrossNodes() = runBlocking {
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
    fun broadcastToTenant_isRoutedAcrossNodes() = runBlocking {
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
    fun unicast_remoteSession_returnsLocalFalseButRemoteDelivers() = runBlocking {
        val cluster = newCluster()
        val sB = RecordingSession("s-B").also(cluster.regB::register)

        // A unicasts to a sessionId that is registered on B, not A.
        val localOk = cluster.distA.unicast("s-B", "ping")

        assertFalse(localOk, "A's local registry does not hold s-B → unicast returns local-false")
        assertEquals(listOf("ping"), sB.received, "B still receives the message via channel-backed delivery")
    }

    @Test
    fun selfEcho_doesNotDoubleDeliverOnOriginatingNode() = runBlocking {
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
    fun inboundDispatch_failureDoesNotTerminateSubscription() = runBlocking {
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
    fun unicast_localSession_returnsTrue() = runBlocking {
        val cluster = newCluster()
        val sA = RecordingSession("s-A").also(cluster.regA::register)

        assertTrue(cluster.distA.unicast("s-A", "to-self"), "Local registry holds s-A → unicast returns true")
        assertEquals(listOf("to-self"), sA.received)
    }

    @Test
    fun publishFailure_isSwallowed_andLocalDeliveryStillHappens() = runBlocking {
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
    fun inbound_nullTargetId_forTargetedTypes_isDroppedSilently() = runBlocking {
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
    fun inbound_localBroadcasterThrowing_isCaught_andListenerSurvives() = runBlocking {
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
    fun envelope_carriesNodeIdTargetAndText() = runBlocking {
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
        override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit) { this.handler = handler }
    }

    /** publish always throws; subscribe is accepted — models a down transport. */
    private class CrashingChannel : IWebSocketBroadcastChannel {
        override suspend fun publish(envelope: WebSocketBroadcastEnvelope): Unit = error("transport down")
        override fun subscribe(handler: suspend (WebSocketBroadcastEnvelope) -> Unit) {}
    }

    private data class Cluster(
        val regA: KudosWebSocketRegistry,
        val regB: KudosWebSocketRegistry,
        val distA: DistributedWebSocketBroadcaster,
        val distB: DistributedWebSocketBroadcaster,
        val channel: InMemoryBroadcastChannel,
    )

    /** Captures `sendText` payloads in order; thread-safe under the concurrent broadcaster. */
    private class RecordingSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        val received: MutableList<String> = CopyOnWriteArrayList()
        override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()
        override suspend fun sendText(text: String) { received += text }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: CloseReason) {}
    }

    /** Always throws on send — used to assert one bad session does not poison the channel subscription. */
    private class CrashingSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        var attempts: Int = 0
            private set
        override val attributes: MutableMap<String, Any?> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {
            attempts++
            error("boom")
        }
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: CloseReason) {}
    }
}
